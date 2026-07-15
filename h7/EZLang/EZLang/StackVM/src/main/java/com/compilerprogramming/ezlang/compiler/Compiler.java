package com.compilerprogramming.ezlang.compiler;

import com.compilerprogramming.ezlang.lexer.Lexer;
import com.compilerprogramming.ezlang.parser.AST;
import com.compilerprogramming.ezlang.parser.Parser;
import com.compilerprogramming.ezlang.semantic.NullableAnalysis;
import com.compilerprogramming.ezlang.semantic.SemaAssignTypes;
import com.compilerprogramming.ezlang.semantic.SemaDefineTypes;
import com.compilerprogramming.ezlang.types.Symbol;
import com.compilerprogramming.ezlang.types.EZType;
import com.compilerprogramming.ezlang.types.TypeDictionary;

import java.util.BitSet;

public class Compiler {

    private void compile(TypeDictionary typeDictionary) {
        for (Symbol symbol: typeDictionary.getLocalSymbols()) {
            if (symbol instanceof Symbol.FunctionTypeSymbol) {
                Symbol.FunctionTypeSymbol functionSymbol = (Symbol.FunctionTypeSymbol) symbol;
                EZType.EZTypeFunction functionType = (EZType.EZTypeFunction) functionSymbol.type;
                functionType.code = new CompiledFunction(functionSymbol);
            }
        }
    }
    public TypeDictionary compileSrc(String src) {
        Parser parser = new Parser();
        AST.Program program = parser.parse(new Lexer(src));
        TypeDictionary typeDict = new TypeDictionary();
        SemaDefineTypes sema = new SemaDefineTypes(typeDict);
        sema.analyze(program);
        SemaAssignTypes sema2 = new SemaAssignTypes(typeDict);
        sema2.analyze(program);
        NullableAnalysis.analyze(typeDict);
        compile(typeDict);
        return typeDict;
    }
    public String dumpIR(TypeDictionary typeDictionary) {
        StringBuilder sb = new StringBuilder();
        for (Symbol s: typeDictionary.bindings.values()) {
            if (s instanceof Symbol.FunctionTypeSymbol) {
                Symbol.FunctionTypeSymbol f = (Symbol.FunctionTypeSymbol) s;
                CompiledFunction functionBuilder = (CompiledFunction) f.code();
                BasicBlock.toStr(sb, functionBuilder.entry, new BitSet());
            }
        }
        return sb.toString();
    }
}
