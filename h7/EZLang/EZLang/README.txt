
## cloned to LightSun/ez-lang
The project is under development and subject to change. At this point in time, we have following initial implementations:

lexer - a simple tokenizer.
parser - a recursive descent parser and AST.
types - the type definitions.
semantic - semantic analyzer.
stackvm - a compiler that generates IR for a stack based virtual machine. There is no interpreter for this instruction set yet.
registervm - a compiler that generates a so called three-address IR and an interpreter that can execute the IR.
optvm - WIP optimizing compiler with SSA transformation, constant propagation, graph coloring register allocation targeting an abstract machine. Includes Interpreter to run the abstract machine.
seaofnodes - WIP compiler that generates Sea of Nodes IR, using SoN backend from Simple Chapter 21. Generates native code for X86-64, AArch64 and RISC-V.



专业翻译（编译器教学项目文档）
本项目仍处于开发阶段，接口与功能均可能发生变动。目前已完成以下基础模块实现：
lexer（词法分析器）：简易分词器
parser（语法分析器）：递归下降解析器，配套抽象语法树（AST）
types（类型模块）：全部类型定义
semantic（语义分析模块）：语义分析器
stackvm（栈虚拟机编译模块）：生成栈式虚拟机中间表示（IR）的编译器；该指令集暂未配套解释器
registervm（寄存器虚拟机编译模块）：生成三地址中间表示（three-address IR）的编译器，附带可执行该 IR 的解释器
optvm（优化编译器模块，开发中）：支持 SSA 变换、常量传播、图着色寄存器分配，面向抽象机器；内置抽象机器解释器
seaofnodes（节点海编译器，开发中）：编译器，功能尚未写完

cloned 2026.7.8