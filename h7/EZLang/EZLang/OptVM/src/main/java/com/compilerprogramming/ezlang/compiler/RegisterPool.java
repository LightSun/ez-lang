package com.compilerprogramming.ezlang.compiler;

import com.compilerprogramming.ezlang.types.EZType;

import java.util.ArrayList;

/**
 * The RegisterPool is used when compiling functions
 * to assign IDs to registers. Initially the registers get
 * sequential IDs. For SSA registers we assign new IDs but also
 * retain the old ID and attach a version number - the old ID is
 * required because our SSA algo must be able to track each original
 * variable / register.
 */
public class RegisterPool {
    private final ArrayList<Register> registers = new ArrayList<>();

    public Register getReg(int regNumber) {
        return registers.get(regNumber);
    }
    public Register newReg(String baseName, EZType type) {
        int id = registers.size();
        Register reg = new Register(id, baseName, type);
        registers.add(reg);
        return reg;
    }
    public Register newTempReg(EZType type) {
        int id = registers.size();
        String name = "%t"+id;
        Register reg = new Register(id, name, type);
        registers.add(reg);
        return reg;
    }
    public Register newTempReg(String baseName, EZType type) {
        int id = registers.size();
        String name = baseName+"_"+id;
        Register reg = new Register(id, name, type);
        registers.add(reg);
        return reg;
    }
    public Register.SSARegister ssaReg(Register original, int version) {
        int id = registers.size();
        Register.SSARegister reg = new Register.SSARegister(original, id, version);
        registers.add(reg);
        return reg;
    }
    public int numRegisters() {
        return registers.size();
    }
    public void toStr(StringBuilder sb) {
        for (Register reg : registers) {
            sb.append("Reg #").append(reg.id).append(" ").append(reg.name()).append(" ").append(reg.nonSSAId()).append("\n");
        }
    }
}
