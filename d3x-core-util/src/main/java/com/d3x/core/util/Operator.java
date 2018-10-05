/*
 * Copyright (C) 2018 D3X Systems - All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.d3x.core.util;

/**
 * An enum that defines various kinds of operators
 *
 * @author Xavier Witdouck
 */
public enum Operator {

    EQ("=="),
    NE("!="),
    GT(">"),
    GE(">="),
    LT("<"),
    LE("<=");

    private String symbol;

    /**
     * Constructor
     * @param symbol    the symbol for this operator
     */
    Operator(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Returns the symbol for this operator
     * @return      the symbol for operator
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Returns the operator for the symbol specified
     * @param symbol    the operator symbol
     * @return          the operator match
     * @throws IllegalArgumentException if no match for symbol
     */
    public static Operator of(String symbol) {
        if (symbol == null) throw new IllegalArgumentException("The operator symbol cannot be null");
        else if (symbol.equals("=="))   return EQ;
        else if (symbol.equals("!="))   return NE;
        else if (symbol.equals(">="))   return GE;
        else if (symbol.equals(">"))    return GT;
        else if (symbol.equals("<="))   return LE;
        else if (symbol.equals("<"))    return LT;
        else throw new IllegalArgumentException("Unrecognized operator symbol: " + symbol);
    }
}

