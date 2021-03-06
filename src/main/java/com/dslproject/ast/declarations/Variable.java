package com.dslproject.ast.declarations;

import com.dslproject.libs.DslVisitor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class Variable extends Declaration {
    private List<Note> notes;

    private String instrument;

    private Integer tempo;

    public Variable(String name, List<Note> notes, String instrument, Integer tempo) {
        super(name);
        this.notes = notes;
        this.instrument = instrument;
        this.tempo = tempo;
    }

    public String getNotesString(){
        StringBuilder noteStr = new StringBuilder();
        for (Note note: notes) {
            noteStr.append(" ").append(note.toString());
        }
        return noteStr.toString();
    }

    public String getRestNoteString(){
        StringBuilder noteStr = new StringBuilder();
        for (Note note : notes) {
            noteStr.append(" R").append(note.getLength());
        }
        return noteStr.toString().trim();
    }

    @Override
    public Integer getBeats() {
        int beats = 0;
        for (Note note : notes) {
            beats += note.getBeats();
        }
        return beats;
    }

    @Override
    public boolean validateStructure() {
        return true;
    }

    public int getNotesSize(){
        return notes.size();
    }

    @Override
    public <T, C> T accept(C context, DslVisitor<T, C> v) {
        return v.visit(context, this);
    }
}
