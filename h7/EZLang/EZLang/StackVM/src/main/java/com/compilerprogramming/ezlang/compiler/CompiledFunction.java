package com.compilerprogramming.ezlang.compiler;

import com.compilerprogramming.ezlang.compiler.utils.CompileHelper;
import com.compilerprogramming.ezlang.exceptions.CompilerException;
import com.compilerprogramming.ezlang.parser.AST;
import com.compilerprogramming.ezlang.types.Scope;
import com.compilerprogramming.ezlang.types.Symbol;
import com.compilerprogramming.ezlang.types.EZType;

public class CompiledFunction {

    public BasicBlock entry;
    public BasicBlock exit;
    private int bid = 0;
    private BasicBlock currentBlock;
    private BasicBlock currentBreakTarget;
    private BasicBlock currentContinueTarget;

    private class CompiledFunctionImpl implements IStatementCompiler{
        @Override
        public void compileBlock(AST.BlockStmt statement) {
            CompiledFunction.this.compileBlock(statement);
        }
        @Override
        public void compileLet(AST.VarStmt statement) {
            CompiledFunction.this.compileLet(statement);
        }
        @Override
        public void compileIf(AST.IfElseStmt statement) {
            CompiledFunction.this.compileIf(statement);
        }
        @Override
        public void compileWhile(AST.WhileStmt statement) {
            CompiledFunction.this.compileWhile(statement);
        }
        @Override
        public void compileContinue(AST.ContinueStmt statement) {
            CompiledFunction.this.compileContinue(statement);
        }
        @Override
        public void compileBreak(AST.BreakStmt statement) {
            CompiledFunction.this.compileBreak(statement);
        }
        @Override
        public void compileReturn(AST.ReturnStmt statement) {
            CompiledFunction.this.compileReturn(statement);
        }
        @Override
        public void compileAssign(AST.AssignStmt statement) {
            CompiledFunction.this.compileAssign(statement);
        }
        @Override
        public void compileExprStmt(AST.ExprStmt statement) {
            CompiledFunction.this.compileExprStmt(statement);
        }
    }

    public CompiledFunction(Symbol.FunctionTypeSymbol functionSymbol) {
        AST.FuncDecl funcDecl = (AST.FuncDecl) functionSymbol.functionDecl;
        setVirtualRegisters(funcDecl.scope);
        this.bid = 0;
        this.entry = this.currentBlock = createBlock();
        this.exit = createBlock();
        this.currentBreakTarget = null;
        this.currentContinueTarget = null;
        compileStatement(funcDecl.block);
        exitBlockIfNeeded();
    }
	
    private void exitBlockIfNeeded() {
        if (currentBlock != null &&
                currentBlock != exit) {
            startBlock(exit);
        }
    }

    private void setVirtualRegisters(Scope scope) {
        int reg = 0;
        if (scope.parent != null)
            reg = scope.parent.maxReg;
        for (Symbol symbol: scope.getLocalSymbols()) {
            if (symbol instanceof Symbol.VarSymbol) {
                ((Symbol.VarSymbol)symbol).regNumber = reg++;
            }
        }
        scope.maxReg = reg;
        for (Scope childScope: scope.children) {
            setVirtualRegisters(childScope);
        }
    }

    private BasicBlock createBlock() {
        return new BasicBlock(bid++);
    }

    private BasicBlock createLoopHead() {
        return new BasicBlock(bid++, true);
    }

    private void compileBlock(AST.BlockStmt block) {
        for (AST.Stmt stmt: block.stmtList) {
            compileStatement(stmt);
        }
    }

    private void compileReturn(AST.ReturnStmt returnStmt) {
        if (returnStmt.expr != null) {
            boolean indexed = compileExpr(returnStmt.expr);
            if (indexed)
                codeIndexedLoad();
        }
        jumpTo(exit);
    }

    private void code(Instruction instruction) {
        currentBlock.add(instruction);
    }

    private void compileStatement(AST.Stmt statement) {
        CompileHelper.compileStatement(statement, new CompiledFunctionImpl());
//        switch (statement) {
//            case AST.BlockStmt blockStmt -> {
//                compileBlock(blockStmt);
//            }
//            case AST.VarStmt letStmt -> {
//                compileLet(letStmt);
//            }
//            case AST.VarDeclStmt varDeclStmt -> {}
//            case AST.IfElseStmt ifElseStmt -> {
//                compileIf(ifElseStmt);
//            }
//            case AST.WhileStmt whileStmt -> {
//                compileWhile(whileStmt);
//            }
//            case AST.ContinueStmt continueStmt -> {
//                compileContinue(continueStmt);
//            }
//            case AST.BreakStmt breakStmt -> {
//                compileBreak(breakStmt);
//            }
//            case AST.ReturnStmt returnStmt -> {
//                compileReturn(returnStmt);
//            }
//            case AST.AssignStmt assignStmt -> {
//                compileAssign(assignStmt);
//            }
//            case AST.ExprStmt exprStmt -> {
//                compileExprStmt(exprStmt);
//            }
//            default -> throw new IllegalStateException("Unexpected value: " + statement);
//        }
    }

    private void compileAssign(AST.AssignStmt assignStmt) {
        boolean indexedRhs = compileExpr(assignStmt.rhs);
        if (indexedRhs)
            codeIndexedLoad();
        Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) assignStmt.nameExpr.symbol;
        code(new Instruction.Store(varSymbol.regNumber));
    }

    private void compileExprStmt(AST.ExprStmt exprStmt) {
        boolean indexed = compileExpr(exprStmt.expr);
        if (indexed)
            codeIndexedLoad();
        code(new Instruction.Pop());
    }

    private void compileContinue(AST.ContinueStmt continueStmt) {
        if (currentContinueTarget == null)
            throw new CompilerException("No continue target found", continueStmt.lineNumber);
        jumpTo(currentContinueTarget);
    }

    private void compileBreak(AST.BreakStmt breakStmt) {
        if (currentBreakTarget == null)
            throw new CompilerException("No break target found", breakStmt.lineNumber);
        jumpTo(currentBreakTarget);
    }

    private void compileWhile(AST.WhileStmt whileStmt) {
        BasicBlock loopHead = createLoopHead();
        BasicBlock bodyBlock = createBlock();
        BasicBlock exitBlock = createBlock();
        BasicBlock savedBreakTarget = currentBreakTarget;
        BasicBlock savedContinueTarget = currentContinueTarget;
        currentBreakTarget = exitBlock;
        currentContinueTarget = loopHead;
        startBlock(loopHead);
        boolean indexed = compileExpr(whileStmt.condition);
        if (indexed)
            codeIndexedLoad();
        code(new Instruction.ConditionalBranch(currentBlock, bodyBlock, exitBlock));
        startBlock(bodyBlock);
        compileStatement(whileStmt.stmt);
        if (!isBlockTerminated(currentBlock))
            jumpTo(loopHead);
        startBlock(exitBlock);
        currentContinueTarget = savedContinueTarget;
        currentBreakTarget = savedBreakTarget;
    }

    private boolean isBlockTerminated(BasicBlock block) {
        return (block.instructions.size() > 0 &&
                block.instructions.get(block.instructions.size()-1).isTerminal());
    }

    private void jumpTo(BasicBlock block) {
        assert !isBlockTerminated(currentBlock);
        currentBlock.add(new Instruction.Jump(block));
        currentBlock.addSuccessor(block);
    }

    private void startBlock(BasicBlock block) {
        if (!isBlockTerminated(currentBlock)) {
            jumpTo(block);
        }
        currentBlock = block;
    }

    private void compileIf(AST.IfElseStmt ifElseStmt) {
        BasicBlock thenBlock = createBlock();
        boolean needElse = ifElseStmt.elseStmt != null;
        BasicBlock elseBlock = needElse ? createBlock() : null;
        BasicBlock exitBlock = createBlock();
        boolean indexed = compileExpr(ifElseStmt.condition);
        if (indexed)
            codeIndexedLoad();
        code(new Instruction.ConditionalBranch(currentBlock, thenBlock, needElse ? elseBlock : exitBlock));
        startBlock(thenBlock);
        compileStatement(ifElseStmt.ifStmt);
        if (!isBlockTerminated(currentBlock))
            jumpTo(exitBlock);
        if (elseBlock != null) {
            startBlock(elseBlock);
            compileStatement(ifElseStmt.elseStmt);
            if (!isBlockTerminated(currentBlock))
                jumpTo(exitBlock);
        }
        startBlock(exitBlock);
    }

    private void compileLet(AST.VarStmt letStmt) {
        if (letStmt.expr != null) {
            boolean indexed = compileExpr(letStmt.expr);
            if (indexed)
                codeIndexedLoad();
            code(new Instruction.Store(letStmt.symbol.regNumber));
        }
    }

    private boolean compileExpr(AST.Expr expr) {
        if(expr instanceof AST.LiteralExpr){
            return compileConstantExpr((AST.LiteralExpr)expr);
        }
        else if(expr instanceof AST.BinaryExpr){
            return compileBinaryExpr((AST.BinaryExpr)expr);
        }else if(expr instanceof AST.UnaryExpr){
            return compileUnaryExpr((AST.UnaryExpr)expr);
        }else if(expr instanceof AST.NameExpr){
            return compileSymbolExpr((AST.NameExpr)expr);
        }else if(expr instanceof AST.NewExpr){
            return compileNewExpr((AST.NewExpr)expr);
        }else if(expr instanceof AST.InitExpr){
            return compileInitExpr((AST.InitExpr)expr);
        }else if(expr instanceof AST.ArrayLoadExpr){
            return compileArrayIndexExpr((AST.ArrayLoadExpr)expr);
        }else if(expr instanceof AST.ArrayStoreExpr){
            return compileArrayStoreExpr((AST.ArrayStoreExpr)expr);
        }else if(expr instanceof AST.GetFieldExpr){
            return compileFieldExpr((AST.GetFieldExpr)expr);
        }else if(expr instanceof AST.SetFieldExpr){
            return compileSetFieldExpr((AST.SetFieldExpr)expr);
        }else if(expr instanceof AST.CallExpr){
            return compileCallExpr((AST.CallExpr)expr);
        }else{
            throw new IllegalStateException("Unexpected value: " + expr);
        }
//        switch (expr) {
//            case AST.LiteralExpr constantExpr -> {
//                return compileConstantExpr(constantExpr);
//            }
//            case AST.BinaryExpr binaryExpr -> {
//                return compileBinaryExpr(binaryExpr);
//            }
//            case AST.UnaryExpr unaryExpr -> {
//                return compileUnaryExpr(unaryExpr);
//            }
//            case AST.NameExpr symbolExpr -> {
//                return compileSymbolExpr(symbolExpr);
//            }
//            case AST.NewExpr newExpr -> {
//                return compileNewExpr(newExpr);
//            }
//            case AST.InitExpr initExpr -> {
//                return compileInitExpr(initExpr);
//            }
//            case AST.ArrayLoadExpr arrayLoadExpr -> {
//                return compileArrayIndexExpr(arrayLoadExpr);
//            }
//            case AST.ArrayStoreExpr arrayStoreExpr -> {
//                return compileArrayStoreExpr(arrayStoreExpr);
//            }
//            case AST.GetFieldExpr getFieldExpr -> {
//                return compileFieldExpr(getFieldExpr);
//            }
//            case AST.SetFieldExpr setFieldExpr -> {
//                return compileSetFieldExpr(setFieldExpr);
//            }
//            case AST.CallExpr callExpr -> {
//                return compileCallExpr(callExpr);
//            }
//            default -> throw new IllegalStateException("Unexpected value: " + expr);
//        }
    }

    private boolean compileCallExpr(AST.CallExpr callExpr) {
        compileExpr(callExpr.callee);
        for (AST.Expr expr: callExpr.args) {
            boolean indexed = compileExpr(expr);
            if (indexed)
                codeIndexedLoad();
        }
        code(new Instruction.Call(callExpr.args.size()));
        return false;
    }

    private EZType.EZTypeStruct getStructType(EZType t, int lineNumber) {
        if (t instanceof EZType.EZTypeStruct) {
            return (EZType.EZTypeStruct) t;
        }
        else if (t instanceof EZType.EZTypeNullable &&
                ((EZType.EZTypeNullable)t).baseType instanceof EZType.EZTypeStruct) {
            return (EZType.EZTypeStruct) ((EZType.EZTypeNullable)t).baseType;
        }
        else
            throw new CompilerException("Unexpected type: " + t, lineNumber);
    }

    private boolean compileFieldExpr(AST.GetFieldExpr fieldExpr) {
        EZType.EZTypeStruct typeStruct = getStructType(fieldExpr.object.type, fieldExpr.lineNumber);
        int fieldIndex = typeStruct.getFieldIndex(fieldExpr.fieldName);
        if (fieldIndex < 0)
            throw new CompilerException("Field " + fieldExpr.fieldName + " not found", fieldExpr.lineNumber);
        boolean indexed = compileExpr(fieldExpr.object);
        if (indexed)
            codeIndexedLoad();
        code(new Instruction.PushConst(fieldIndex));
        return true;
    }

    private boolean compileArrayIndexExpr(AST.ArrayLoadExpr arrayIndexExpr) {
        compileExpr(arrayIndexExpr.array);
        boolean indexed = compileExpr(arrayIndexExpr.expr);
        if (indexed)
            codeIndexedLoad();
        return true;
    }

    private boolean compileSetFieldExpr(AST.SetFieldExpr setFieldExpr) {
        EZType.EZTypeStruct structType = (EZType.EZTypeStruct) setFieldExpr.object.type;
        int fieldIndex = structType.getFieldIndex(setFieldExpr.fieldName);
        if (fieldIndex == -1)
            throw new CompilerException("Field " + setFieldExpr.fieldName + " not found in struct " + structType.name, setFieldExpr.lineNumber);
        if (!(setFieldExpr instanceof AST.InitFieldExpr))
            compileExpr(setFieldExpr.object);
        code(new Instruction.PushConst(fieldIndex));
        boolean indexed = compileExpr(setFieldExpr.value);
        if (indexed)
            codeIndexedLoad();
        codeIndexedStore();
        return false;
    }

    private boolean compileArrayStoreExpr(AST.ArrayStoreExpr arrayStoreExpr) {
        if (!(arrayStoreExpr instanceof AST.ArrayInitExpr))
            compileExpr(arrayStoreExpr.array);
        boolean indexed = compileExpr(arrayStoreExpr.expr);
        if (indexed)
            codeIndexedLoad();
        indexed = compileExpr(arrayStoreExpr.value);
        if (indexed)
            codeIndexedLoad();
        codeIndexedStore();
        return false;
    }

    private boolean compileNewExpr(AST.NewExpr newExpr) {
        code(new Instruction.New(newExpr.type));
        return false;
    }

    private boolean compileInitExpr(AST.InitExpr initExpr) {
        compileExpr(initExpr.newExpr);
        if (initExpr.initExprList != null && !initExpr.initExprList.isEmpty()) {
            for (AST.Expr expr : initExpr.initExprList) {
                compileExpr(expr);
            }
        }
        return false;
    }

    private boolean compileSymbolExpr(AST.NameExpr symbolExpr) {
        if (symbolExpr.type instanceof EZType.EZTypeFunction)
            code(new Instruction.LoadFunction((EZType.EZTypeFunction)symbolExpr.type));
        else {
            Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) symbolExpr.symbol;
            code(new Instruction.LoadVar(varSymbol.regNumber));
        }
        return false;
    }

    private boolean codeBoolean(AST.BinaryExpr binaryExpr) {
        boolean isAnd = binaryExpr.op.str.equals("&&");
        BasicBlock l1 = createBlock();
        BasicBlock l2 = createBlock();
        BasicBlock l3 = createBlock();
        boolean indexed = compileExpr(binaryExpr.expr1);
        if (indexed)
            codeIndexedLoad();
        if (isAnd) {
            code(new Instruction.ConditionalBranch(currentBlock, l1, l2));
        } else {
            code(new Instruction.ConditionalBranch(currentBlock, l2, l1));
        }
        startBlock(l1);
        indexed = compileExpr(binaryExpr.expr2);
        if (indexed)
            codeIndexedLoad();
        jumpTo(l3);
        startBlock(l2);
        // Below we must write to the same temp
        code(new Instruction.PushConst(isAnd ? 0 : 1));
        jumpTo(l3);
        startBlock(l3);
        // leaves result on stack
        return false;
    }

    private boolean compileBinaryExpr(AST.BinaryExpr binaryExpr) {
        String binOp = binaryExpr.op.str;
        if (binOp.equals("&&") ||
                binOp.equals("||")) {
            return codeBoolean(binaryExpr);
        }
        int opCode = 0;
        boolean indexed = compileExpr(binaryExpr.expr1);
        if (indexed)
            codeIndexedLoad();
        indexed = compileExpr(binaryExpr.expr2);
        if (indexed)
            codeIndexedLoad();
        switch (binOp) {
            case "+" : opCode = Instruction.ADD_I;break;
            case "-" : opCode = Instruction.SUB_I;break;
            case "*" : opCode = Instruction.MUL_I;break;
            case "/" : opCode = Instruction.DIV_I;break;
            case "%" : opCode = Instruction.MOD_I;break;
            case "==" : opCode = Instruction.EQ;break;
            case "!=" : opCode = Instruction.NE;break;
            case "<" : opCode = Instruction.LT;break;
            case ">" : opCode = Instruction.GT;break;
            case "<=" : opCode = Instruction.LE;break;
            case ">=" : opCode = Instruction.GE;break;
            default : throw new CompilerException("Invalid binary op", binaryExpr.lineNumber);
        }
        code(new Instruction.BinaryOp(opCode));
        return false;
    }

    private boolean compileUnaryExpr(AST.UnaryExpr unaryExpr) {
        int opCode = 0;
        boolean indexed = compileExpr(unaryExpr.expr);
        if (indexed)
            code(new Instruction.LoadIndexed());
        switch (unaryExpr.op.str) {
            case "-" : opCode = Instruction.NEG_I;break;
            case "!" : opCode = Instruction.NOT;break;
            default : throw new CompilerException("Invalid binary op", unaryExpr.lineNumber);
        }
        code(new Instruction.UnaryOp(opCode));
        return false;
    }

    private boolean compileConstantExpr(AST.LiteralExpr constantExpr) {
        code(new Instruction.PushConst(constantExpr.value.num.intValue()));
        return false;
    }

    private void codeIndexedLoad() {
        code(new Instruction.LoadIndexed());
    }

    private void codeIndexedStore() {
        code(new Instruction.StoreIndexed());
    }
}
