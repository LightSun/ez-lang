package com.compilerprogramming.ezlang.compiler;

import java.util.*;

/**
 * Implement method to exit SSA by converting to conventional SSA,
 * without coalescing. This is the basic form - it creates extra copies but
 * the transformation is simple and correct, and does not suffer from the lost copy or
 * swap problem.
 *
 * The simple conversion is essentially Method 1 described by
 * Translating Out of Static Single Assignment Form
 * Vugranam C. Sreedhar, Roy Dz-Ching Ju, David M. Gillies, and Vatsa Santhanam
 *
 * However, Sreedhar left out details such as using parallel copies
 * and sequencing of parallel copies.
 *
 * Revisiting Out-of-SSA Translation for Correctness, Code Quality, and Efficiency
 * Benoit Boissinot, Alain Darte, Fabrice Rastello, Benoît Dupont de Dinechin, Christophe Guillon
 *
 * The Boissinot paper gives a more correct description, discussing the need for parallel copy
 * and sequencing the parallel copy. Much of the Boissinot paper is about optimizing away any extra copies, but
 * it seems to me that the Briggs algorithm achieves that anyway while being simpler
 * to implement.
 *
 * Engineering a Compiler, 3rd edition, describes the simpler form - omitting the coalescing part.
 * Our implementation is similar to EaC.
 *
 * We do not use the sequencing algo described in Boissinot paper. Instead, we use:
 *
 * https://xavierleroy.org/publi/parallel-move.pdf
 * Tilting at windmills with Coq: formal verification of a compilation algorithm for parallel moves
 * Laurence Rideau, Bernard Paul Serpette, Xavier Leroy
 */
public class ExitSSABoissinotNoCoalesce {

    CompiledFunction function;
    Map<BasicBlock,PCopy> parallelCopies = new HashMap<>();
    List<BasicBlock> allBlocks;

    public ExitSSABoissinotNoCoalesce(CompiledFunction function, EnumSet<Options> options) {
        this.function = function;
        allBlocks = function.getBlocks();
        init();
        makeConventionalSSA();
        if (options.contains(Options.DUMP_SSA_TO_CSSA)) function.dumpIR(false, "After converting from SSA to CSSA");
        removePhis();
        if (options.contains(Options.DUMP_CSSA_PHI_REMOVAL)) function.dumpIR(false, "After removing phis from CSSA");
        sequenceParallelCopies();
        function.isSSA = false;
        if (options.contains(Options.DUMP_POST_SSA_IR)) function.dumpIR(false, "After exiting SSA (Boissinot method)");
    }

    private void init() {
        // We do not actually insert parallel copy instruction until needed
        // but we create an auxiliary data structure to help us track these
        for (BasicBlock block: allBlocks) {
            parallelCopies.put(block,new PCopy(block));
        }
    }
    private void insertAtEnd(BasicBlock bb, Instruction i) {
        assert bb.instructions.size() > 0;
        // Last instruction is a branch - so new instruction will
        // go before that
        int pos = bb.instructions.size()-1;
        bb.add(pos, i);
    }

    private Instruction.ParallelCopyInstruction getParallelCopyAtEnd(BasicBlock block) {
        PCopy pcopy = parallelCopies.get(block);
        if (pcopy.pCopyEnd == null) {
            pcopy.pCopyEnd = new Instruction.ParallelCopyInstruction();
            insertAtEnd(block,pcopy.pCopyEnd);
        }
        return pcopy.pCopyEnd;
    }

    private void insertAfterPhis(BasicBlock bb, Instruction newInst) {
        assert bb.instructions.size() > 0;
        int insertionPos = -1;
        for (int pos = 0; pos < bb.instructions.size(); pos++) {
            Instruction i = bb.instructions.get(pos);
            if (i instanceof Instruction.Phi) {
                insertionPos = pos+1;   // After phi
            }
            else
                break;
        }
        if (insertionPos < 0) {
            throw new IllegalStateException();
        }
        bb.add(insertionPos, newInst);
    }

    private Instruction.ParallelCopyInstruction getParallelCopyAtBegin(BasicBlock block) {
        PCopy pcopy = parallelCopies.get(block);
        if (pcopy.pCopyBegin == null) {
            pcopy.pCopyBegin = new Instruction.ParallelCopyInstruction();
            insertAfterPhis(block,pcopy.pCopyBegin);
        }
        return pcopy.pCopyBegin;
    }

    /**
     * Isolate phi nodes to make SSA conventional.
     * This is Phase 1 as described in Engineering a Compiler 3rd Edition, p490.
     * It is also described as method 1 by Sreedhar, and explained in detail by Boissinot.
     */
    private void makeConventionalSSA() {
        List<BasicBlock> blocks = function.getBlocks();
        for (BasicBlock block: blocks) {
            List<Instruction.Phi> phis = block.phis();
            if (phis.isEmpty())
                continue;
            for (Instruction.Phi phi: phis) {
                for (int j = 0; j < phi.numInputs(); j++) {
                    BasicBlock pred = block.predecessor(j);
                    Instruction.ParallelCopyInstruction pCopyBEnd = getParallelCopyAtEnd(pred);
                    Operand oldInput = phi.input(j);
                    //
                    Operand.RegisterOperand regInput = null;
                    if(oldInput instanceof Operand.RegisterOperand){
                        regInput = (Operand.RegisterOperand) oldInput;
                    }
                    Register newInput = oldInput instanceof Operand.RegisterOperand ?
                            function.registerPool.newTempReg(regInput.reg.name(), oldInput.type) :
                            function.registerPool.newTempReg(oldInput.type);
                    pCopyBEnd.addCopy(oldInput,new Operand.RegisterOperand(newInput));
                    phi.replaceInput(j,newInput);
                }
                Register oldPhiVar = phi.value();
                Register newPhiVar = function.registerPool.newTempReg(oldPhiVar.name(), oldPhiVar.type);
                phi.replaceValue(newPhiVar);
                Instruction.ParallelCopyInstruction pCopyBBegin = getParallelCopyAtBegin(block);
                pCopyBBegin.addCopy(new Operand.RegisterOperand(newPhiVar),new Operand.RegisterOperand(oldPhiVar));
            }
        }
    }

    /**
     * Phase 2 in Engineering a Compiler
     */
    private void removePhis() {
        List<BasicBlock> blocks = function.getBlocks();
        for (BasicBlock block: blocks) {
            List<Instruction.Phi> phis = block.phis();
            if (phis.isEmpty())
                continue;
            // Insert copy in predecessor, since we are in CSSA, this is
            // a simple assignment from phi input to phi var
            for (Instruction.Phi phi: phis) {
                for (int j = 0; j < phi.numInputs(); j++) {
                    BasicBlock pred = block.predecessor(j);
                    Operand phiInput = phi.input(j);
                    Register phiVar = phi.value();
                    insertAtEnd(pred,new Instruction.Move(phiInput,new Operand.RegisterOperand(phiVar)));
                }
            }
            block.instructions.removeIf(instruction -> instruction instanceof Instruction.Phi);
        }
    }

    /**
     * Phase 3 in Engineering a Compiler.
     */
    private void sequenceParallelCopies() {
        for (BasicBlock block: function.getBlocks()) {
            PCopy pcopy = parallelCopies.get(block);
            if (pcopy.pCopyBegin != null)
                sequenceParallelCopy(block,pcopy.pCopyBegin);
            if (pcopy.pCopyEnd != null)
                sequenceParallelCopy(block,pcopy.pCopyEnd);
        }
    }

    private void replaceInstruction(BasicBlock block, Instruction.ParallelCopyInstruction pcopy, ArrayList<Instruction> instructions) {
        block.replaceInstruction(pcopy, instructions);
    }

    private boolean isEqual(Operand op1, Operand op2) {
        if (op1 instanceof Operand.RegisterOperand && op2 instanceof Operand.RegisterOperand) {
            Operand.RegisterOperand reg1 = (Operand.RegisterOperand) op1;
            Operand.RegisterOperand reg2 = (Operand.RegisterOperand) op2;
            return reg1.reg.id == reg2.reg.id;
        }
        return false;
    }

    // The parallel move algo below is from
    // https://xavierleroy.org/publi/parallel-move.pdf
    // Tilting at windmills with Coq:
    // formal verification of a compilation algorithm
    // for parallel moves
    // Laurence Rideau, Bernard Paul Serpette, Xavier Leroy

    enum MoveStatus {
        TO_MOVE, BEING_MOVED, MOVED
    }

    static final class MoveCtx {
        Operand[] src;
        Operand[] dst;
        MoveStatus[] status;
        ArrayList<Instruction> copyInstructions;

        MoveCtx(Instruction.ParallelCopyInstruction pcopy) {
            src = pcopy.sourceOperands.toArray(new Operand[0]);
            dst = pcopy.destOperands.toArray(new Operand[0]);
            copyInstructions = new ArrayList<Instruction>();
            status = new MoveStatus[src.length];
            Arrays.fill(status, MoveStatus.TO_MOVE);
        }
    }

    private void moveOne(MoveCtx ctx, int i) {
        Operand[] src = ctx.src;
        Operand[] dst = ctx.dst;
        if (!isEqual(src[i], dst[i])) {
            ctx.status[i] = MoveStatus.BEING_MOVED;
            for (int j = 0; j < src.length; j++) {
                if (isEqual(src[j],dst[i])) {
                    // cycle found
                    switch (ctx.status[j]) {
                        case TO_MOVE:
                            moveOne(ctx, j);
                            break;
                        case BEING_MOVED:
                            Operand.RegisterOperand temp = new Operand.RegisterOperand(function.registerPool.newTempReg(src[j].type));
                            ctx.copyInstructions.add(new Instruction.Move(src[j], temp));
                            src[j] = temp;
                            break;
                        case MOVED:
                            break;
                    }
                }
            }
            ctx.copyInstructions.add(new Instruction.Move(src[i], dst[i]));
            ctx.status[i] = MoveStatus.MOVED;
        }
    }

    private void sequenceParallelCopy(BasicBlock block, Instruction.ParallelCopyInstruction parallelCopyInstruction) {
        MoveCtx ctx = new MoveCtx(parallelCopyInstruction);
        for (int i = 0; i < ctx.src.length; i++)
            if (ctx.status[i] == MoveStatus.TO_MOVE)
                moveOne(ctx,i);
        replaceInstruction(block,parallelCopyInstruction,ctx.copyInstructions);
    }


    static final class PCopy {
        BasicBlock block;
        /**
         * Parallel copy instruction after any Phi instructions
         * in the block, null if not present
         */
        Instruction.ParallelCopyInstruction pCopyBegin = null;
        /**
         * Parallel copy instruction at the end of a block,
         * null if not present
         */
        Instruction.ParallelCopyInstruction pCopyEnd = null;

        public PCopy(BasicBlock block) {
            this.block = block;
        }
    }
}
