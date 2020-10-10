package com.dslproject.ast;

import com.dslproject.libs.Node;

import java.util.ArrayList;
import java.util.List;

public class Program extends Node {
    private List<Statement> statements = new ArrayList<>();

    List<Statement> getStatements() { // default access: all classes in the ast package can access
        return this.statements; // note - we return an alias here! cf. Language Principles lecture, be wary of this: we are trusting the caller not to make unintended changes to this list. An alternative would be to clone the list.
    }

    public Program(List<Statement> statements) {
        this.statements = statements;
    }

    //@Override
    public <T> T accept(DslVisitor<T> v) {
        return v.visit(this);
    }
}