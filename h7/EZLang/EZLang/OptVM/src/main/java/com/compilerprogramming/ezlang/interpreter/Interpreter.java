package com.compilerprogramming.ezlang.interpreter;

import com.compilerprogramming.ezlang.compiler.BasicBlock;
import com.compilerprogramming.ezlang.compiler.CompiledFunction;
import com.compilerprogramming.ezlang.compiler.Instruction;
import com.compilerprogramming.ezlang.compiler.Operand;
import com.compilerprogramming.ezlang.exceptions.CompilerException;
import com.compilerprogramming.ezlang.exceptions.InterpreterException;
import com.compilerprogramming.ezlang.types.Symbol;
import com.compilerprogramming.ezlang.types.EZType;
import com.compilerprogramming.ezlang.types.TypeDictionary;

public class Interpreter {

    TypeDictionary typeDictionary;

    public Interpreter(TypeDictionary typeDictionary) {
        this.typeDictionary = typeDictionary;
    }

    public Value run(String functionName) {
        Symbol symbol = typeDictionary.lookup(functionName);
        if (symbol instanceof Symbol.FunctionTypeSymbol) {
            Symbol.FunctionTypeSymbol functionSymbol = (Symbol.FunctionTypeSymbol) symbol;
            Frame frame = new Frame(functionSymbol);
            ExecutionStack execStack = new ExecutionStack(1024);
            return interpret(execStack, frame);
        }
        else {
            throw new InterpreterException("Unknown function: " + functionName);
        }
    }

    public Value interpret(ExecutionStack execStack, Frame frame) {
        CompiledFunction currentFunction = frame.bytecodeFunction;
        BasicBlock currentBlock = currentFunction.entry;
        int ip = -1;
        int base = frame.base;
        boolean done = false;
        Value returnValue = null;

        while (!done) {
            Instruction instruction;

            ip++;
            instruction = currentBlock.instructions.get(ip);
            if(instruction instanceof Instruction.Ret){
                Instruction.Ret retInst = (Instruction.Ret) instruction;
                if (retInst.value() instanceof Operand.ConstantOperand) {
                    Operand.ConstantOperand constantOperand = (Operand.ConstantOperand) retInst.value();
                    execStack.stack[base] = new Value.IntegerValue(constantOperand.value);
                }
                else if (retInst.value() instanceof Operand.NullConstantOperand) {
                    execStack.stack[base] = new Value.NullValue();
                }
                else if (retInst.value() instanceof Operand.RegisterOperand) {
                    Operand.RegisterOperand registerOperand = (Operand.RegisterOperand) retInst.value();
                    execStack.stack[base] = execStack.stack[base+registerOperand.frameSlot()];
                }
                else throw new IllegalStateException();
                returnValue = execStack.stack[base];
            }else if(instruction instanceof Instruction.Move){
                Instruction.Move moveInst = (Instruction.Move) instruction;
                if (moveInst.to() instanceof Operand.RegisterOperand) {
                    Operand.RegisterOperand toReg = moveInst.to();
                    if (moveInst.from() instanceof Operand.RegisterOperand) {
                        Operand.RegisterOperand fromReg = (Operand.RegisterOperand) moveInst.from();
                        execStack.stack[base + toReg.frameSlot()] = execStack.stack[base + fromReg.frameSlot()];
                    }
                    else if (moveInst.from() instanceof Operand.ConstantOperand) {
                        Operand.ConstantOperand constantOperand = (Operand.ConstantOperand) moveInst.from();
                        execStack.stack[base + toReg.frameSlot()] = new Value.IntegerValue(constantOperand.value);
                    }
                    else if (moveInst.from() instanceof Operand.NullConstantOperand) {
                        execStack.stack[base + toReg.frameSlot()] = new Value.NullValue();
                    }
                    else throw new IllegalStateException();
                }
                else throw new IllegalStateException();
            }else if(instruction instanceof Instruction.Jump){
                Instruction.Jump jumpInst = (Instruction.Jump) instruction;
                currentBlock = jumpInst.jumpTo;
                ip = -1;
                if (currentBlock == currentFunction.exit)
                    done = true;
            }else if(instruction instanceof Instruction.ConditionalBranch){
                Instruction.ConditionalBranch cbrInst = (Instruction.ConditionalBranch) instruction;
                boolean condition;
                if (cbrInst.condition() instanceof Operand.RegisterOperand) {
                    Operand.RegisterOperand registerOperand = (Operand.RegisterOperand) cbrInst.condition();
                    Value value = execStack.stack[base + registerOperand.frameSlot()];
                    if (value instanceof Value.IntegerValue) {
                        Value.IntegerValue integerValue = (Value.IntegerValue) value;
                        condition = integerValue.value != 0;
                    }
                    else {
                        condition = value != null;
                    }
                }
                else if (cbrInst.condition() instanceof Operand.ConstantOperand) {
                    Operand.ConstantOperand constantOperand = (Operand.ConstantOperand) cbrInst.condition();
                    condition = constantOperand.value != 0;
                }
                else throw new IllegalStateException();
                if (condition)
                    currentBlock = cbrInst.trueBlock;
                else
                    currentBlock = cbrInst.falseBlock;
                ip = -1;
                if (currentBlock == currentFunction.exit)
                    done = true;
            }else if(instruction instanceof Instruction.Call){
                Instruction.Call callInst = (Instruction.Call) instruction;
                int baseReg = base+currentFunction.frameSize();
                int reg = baseReg;
                for (Operand arg: callInst.args()) {
                    if (arg instanceof Operand.RegisterOperand) {
                        Operand.RegisterOperand param = (Operand.RegisterOperand) arg;
                        execStack.stack[reg] = execStack.stack[base + param.frameSlot()];
                    }
                    else if (arg instanceof Operand.ConstantOperand) {
                        Operand.ConstantOperand constantOperand = (Operand.ConstantOperand) arg;
                        execStack.stack[reg] = new Value.IntegerValue(constantOperand.value);
                    }
                    else if (arg instanceof Operand.NullConstantOperand) {
                        execStack.stack[reg] = new Value.NullValue();
                    }
                    reg += 1;
                }
                // Call function
                Frame newFrame = new Frame(frame, baseReg, callInst.callee);
                interpret(execStack, newFrame);
                // Copy return value in expected location
                if (!(callInst.callee.returnType instanceof EZType.EZTypeVoid)) {
                    execStack.stack[base + callInst.returnOperand().frameSlot()] = execStack.stack[baseReg];
                }
            }else if(instruction instanceof Instruction.Unary){
                Instruction.Unary unaryInst = (Instruction.Unary) instruction;
                // We don't expect constant here because we fold constants in unary expressions
                Operand.RegisterOperand unaryOperand = (Operand.RegisterOperand) unaryInst.operand();
                Value unaryValue = execStack.stack[base + unaryOperand.frameSlot()];
                if (unaryValue instanceof Value.IntegerValue) {
                    Value.IntegerValue integerValue = (Value.IntegerValue) unaryValue;
                    switch (unaryInst.unop) {
                        case "-": execStack.stack[base + unaryInst.result().frameSlot()] = new Value.IntegerValue(-integerValue.value); break;
                        // Maybe below we should explicitly set Int
                        case "!": execStack.stack[base + unaryInst.result().frameSlot()] = new Value.IntegerValue(integerValue.value==0?1:0); break;
                        default: throw new CompilerException("Invalid unary op");
                    }
                }
                else
                    throw new IllegalStateException("Unexpected unary operand: " + unaryOperand);
            }else if(instruction instanceof Instruction.Binary){
                Instruction.Binary binaryInst = (Instruction.Binary) instruction;
                long x, y;
                long value = 0;
                boolean intOp = true;
                if (binaryInst.binOp.equals("==") || binaryInst.binOp.equals("!=")) {
                    Operand.RegisterOperand nonNullLitOperand = null;
                    if (binaryInst.left() instanceof Operand.NullConstantOperand) {
                        nonNullLitOperand = (Operand.RegisterOperand)binaryInst.right();
                    }
                    else if (binaryInst.right() instanceof Operand.NullConstantOperand) {
                        nonNullLitOperand = (Operand.RegisterOperand)binaryInst.left();
                    }
                    if (nonNullLitOperand != null) {
                        intOp = false;
                        Value otherValue = execStack.stack[base + nonNullLitOperand.frameSlot()];
                        switch (binaryInst.binOp) {
                            case "==": {
                                value = otherValue instanceof Value.NullValue ? 1 : 0;
                                break;
                            }
                            case "!=": {
                                value = otherValue instanceof Value.NullValue ? 0 : 1;
                                break;
                            }
                            default:
                                throw new IllegalStateException();
                        }
                        execStack.stack[base + binaryInst.result().frameSlot()] = new Value.IntegerValue(value);
                    }
                }
                if (intOp) {
                    if (binaryInst.left() instanceof Operand.ConstantOperand) {
                        Operand.ConstantOperand constant = (Operand.ConstantOperand) binaryInst.left();
                        x = constant.value;
                    }
                    else if (binaryInst.left() instanceof Operand.RegisterOperand) {
                        Operand.RegisterOperand registerOperand = (Operand.RegisterOperand) binaryInst.left();
                        x = ((Value.IntegerValue) execStack.stack[base + registerOperand.frameSlot()]).value;
                    }
                    else throw new IllegalStateException();
                    if (binaryInst.right() instanceof Operand.ConstantOperand) {
                        Operand.ConstantOperand constant = (Operand.ConstantOperand) binaryInst.right();
                        y = constant.value;
                    }
                    else if (binaryInst.right() instanceof Operand.RegisterOperand) {
                        Operand.RegisterOperand registerOperand = (Operand.RegisterOperand) binaryInst.right();
                        y = ((Value.IntegerValue) execStack.stack[base + registerOperand.frameSlot()]).value;
                    }
                    else throw new IllegalStateException();
                    switch (binaryInst.binOp) {
                        case "+": value = x + y; break;
                        case "-": value = x - y; break;
                        case "*": value = x * y; break;
                        case "/": value = x / y; break;
                        case "%": value = x % y; break;
                        case "==": value = x == y ? 1 : 0; break;
                        case "!=": value = x != y ? 1 : 0; break;
                        case "<": value = x < y ? 1: 0; break;
                        case ">": value = x > y ? 1 : 0; break;
                        case "<=": value = x <= y ? 1 : 0; break;
                        case ">=": value = x >= y ? 1 : 0; break;
                        default: throw new IllegalStateException();
                    }
                    execStack.stack[base + binaryInst.result().frameSlot()] = new Value.IntegerValue(value);
                }
            }else if(instruction instanceof Instruction.NewArray){
                Instruction.NewArray newArrayInst = (Instruction.NewArray) instruction;
                long size = 0;
                Value initValue = null;
                if (newArrayInst.len() instanceof Operand.ConstantOperand) {
                    Operand.ConstantOperand constantOperand = (Operand.ConstantOperand) newArrayInst.len();
                    size = constantOperand.value;
                }
                else if (newArrayInst.len() instanceof Operand.RegisterOperand) {
                    Operand.RegisterOperand registerOperand = (Operand.RegisterOperand) newArrayInst.len();
                    Value.IntegerValue indexValue = (Value.IntegerValue) execStack.stack[base + registerOperand.frameSlot()];
                    size = (long) indexValue.value;
                }
                if (newArrayInst.initValue() instanceof Operand.ConstantOperand) {
                    Operand.ConstantOperand constantOperand = (Operand.ConstantOperand) newArrayInst.initValue();
                    initValue = new Value.IntegerValue(constantOperand.value);
                }
                else if (newArrayInst.initValue() instanceof Operand.RegisterOperand) {
                    Operand.RegisterOperand registerOperand = (Operand.RegisterOperand) newArrayInst.initValue();
                    initValue = execStack.stack[base + registerOperand.frameSlot()];
                }
                execStack.stack[base + newArrayInst.destOperand().frameSlot()] = new Value.ArrayValue(
                        newArrayInst.type, size, initValue);
            }else if(instruction instanceof Instruction.NewStruct){
                Instruction.NewStruct newStructInst = (Instruction.NewStruct) instruction;
                execStack.stack[base + newStructInst.destOperand().frameSlot()] = new Value.StructValue(newStructInst.type);
            }else if(instruction instanceof Instruction.ArrayStore){
                Instruction.ArrayStore arrayStoreInst = (Instruction.ArrayStore) instruction;
                if (arrayStoreInst.arrayOperand() instanceof Operand.RegisterOperand) {
                    Operand.RegisterOperand arrayOperand = (Operand.RegisterOperand) arrayStoreInst.arrayOperand();
                    Value.ArrayValue arrayValue = (Value.ArrayValue) execStack.stack[base + arrayOperand.frameSlot()];
                    int index = 0;
                    if (arrayStoreInst.indexOperand() instanceof Operand.ConstantOperand) {
                        Operand.ConstantOperand constant = (Operand.ConstantOperand) arrayStoreInst.indexOperand();
                        index = (int) constant.value;
                    }
                    else if (arrayStoreInst.indexOperand() instanceof Operand.RegisterOperand) {
                        Operand.RegisterOperand registerOperand = (Operand.RegisterOperand) arrayStoreInst.indexOperand();
                        Value.IntegerValue indexValue = (Value.IntegerValue) execStack.stack[base + registerOperand.frameSlot()];
                        index = (int) indexValue.value;
                    }
                    else throw new IllegalStateException();
                    Value value;
                    if (arrayStoreInst.sourceOperand() instanceof Operand.ConstantOperand) {
                        Operand.ConstantOperand constantOperand = (Operand.ConstantOperand) arrayStoreInst.sourceOperand();
                        value = new Value.IntegerValue(constantOperand.value);
                    }
                    else if (arrayStoreInst.sourceOperand() instanceof Operand.NullConstantOperand) {
                        value = new Value.NullValue();
                    }
                    else if (arrayStoreInst.sourceOperand() instanceof Operand.RegisterOperand) {
                        Operand.RegisterOperand registerOperand = (Operand.RegisterOperand) arrayStoreInst.sourceOperand();
                        value = execStack.stack[base + registerOperand.frameSlot()];
                    }
                    else throw new IllegalStateException();
                    if (index == arrayValue.values.size())
                        arrayValue.values.add(value);
                    else
                        arrayValue.values.set(index, value);
                } else throw new IllegalStateException();
            }else if(instruction instanceof Instruction.ArrayLoad){
                Instruction.ArrayLoad arrayLoadInst = (Instruction.ArrayLoad) instruction;
                if (arrayLoadInst.arrayOperand() instanceof Operand.RegisterOperand) {
                    Operand.RegisterOperand arrayOperand = (Operand.RegisterOperand) arrayLoadInst.arrayOperand();
                    Value.ArrayValue arrayValue = (Value.ArrayValue) execStack.stack[base + arrayOperand.frameSlot()];
                    if (arrayLoadInst.indexOperand() instanceof Operand.ConstantOperand) {
                        Operand.ConstantOperand constant = (Operand.ConstantOperand) arrayLoadInst.indexOperand();
                        execStack.stack[base + arrayLoadInst.destOperand().frameSlot()] = arrayValue.values.get((int) constant.value);
                    }
                    else if (arrayLoadInst.indexOperand() instanceof Operand.RegisterOperand) {
                        Operand.RegisterOperand registerOperand = (Operand.RegisterOperand) arrayLoadInst.indexOperand();
                        Value.IntegerValue index = (Value.IntegerValue) execStack.stack[base + registerOperand.frameSlot()];
                        execStack.stack[base + arrayLoadInst.destOperand().frameSlot()] = arrayValue.values.get((int) index.value);
                    }
                    else throw new IllegalStateException();
                } else throw new IllegalStateException();

            }else if(instruction instanceof Instruction.SetField){
                Instruction.SetField setFieldInst = (Instruction.SetField) instruction;
                if (setFieldInst.structOperand() instanceof Operand.RegisterOperand) {
                    Operand.RegisterOperand structOperand = (Operand.RegisterOperand) setFieldInst.structOperand();
                    Value.StructValue structValue = (Value.StructValue) execStack.stack[base + structOperand.frameSlot()];
                    int index = setFieldInst.fieldIndex;
                    Value value;
                    if (setFieldInst.sourceOperand() instanceof Operand.ConstantOperand) {
                        Operand.ConstantOperand constant = (Operand.ConstantOperand) setFieldInst.sourceOperand();
                        value = new Value.IntegerValue(constant.value);
                    }
                    else if (setFieldInst.sourceOperand() instanceof Operand.NullConstantOperand) {
                        value = new Value.NullValue();
                    }
                    else if (setFieldInst.sourceOperand() instanceof Operand.RegisterOperand) {
                        Operand.RegisterOperand registerOperand = (Operand.RegisterOperand) setFieldInst.sourceOperand();
                        value = execStack.stack[base + registerOperand.frameSlot()];
                    }
                    else throw new IllegalStateException();
                    structValue.fields[index] = value;
                } else throw new IllegalStateException();
            }else if(instruction instanceof Instruction.GetField){
                Instruction.GetField getFieldInst = (Instruction.GetField) instruction;
                if (getFieldInst.structOperand() instanceof Operand.RegisterOperand) {
                    Operand.RegisterOperand structOperand = (Operand.RegisterOperand) getFieldInst.structOperand();
                    Value.StructValue structValue = (Value.StructValue) execStack.stack[base + structOperand.frameSlot()];
                    int index = getFieldInst.fieldIndex;
                    execStack.stack[base + getFieldInst.destOperand().frameSlot()] = structValue.fields[index];
                } else throw new IllegalStateException();
            }else if(instruction instanceof Instruction.ArgInstruction){

            }else{
                throw new IllegalStateException("Unexpected value: " + instruction);
            }
        }
        return returnValue;
    }

    static class Frame {
        Frame caller;
        int base;
        CompiledFunction bytecodeFunction;

        public Frame(Symbol.FunctionTypeSymbol functionSymbol) {
            this.caller = null;
            this.base = 0;
            this.bytecodeFunction = (CompiledFunction) functionSymbol.code();
        }

        Frame(Frame caller, int base, EZType.EZTypeFunction functionType) {
            this.caller = caller;
            this.base = base;
            this.bytecodeFunction = (CompiledFunction) functionType.code;
        }
    }
}
