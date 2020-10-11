package com.dslproject.libs;

import com.dslproject.ast.Program;
import com.dslproject.ast.Statement;
import com.dslproject.ast.declarations.*;
import com.dslproject.ast.executions.*;
import com.dslproject.exceptions.ParserException;
import com.dslproject.util.DslConstants;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RequiredArgsConstructor
public class DslParser {

    final private Logger log = LoggerFactory.getLogger(DslParser.class);
    private final Tokenizer tokenizer;
    private final Map<String, Declaration> declarationMap = new HashMap<>();
    private final List<Statement> statements = new ArrayList<>();

    public static DslParser getParser(Tokenizer tokenizer) {
        return new DslParser(tokenizer);
    }

    public Program parseProgram() throws ParserException {
        while (tokenizer.moreTokens()) {
            parseStatement();
        }
        return new Program(this.statements);
    }

    private void parseStatement() throws ParserException {
        if (tokenizer.checkToken(DslConstants.NEWLINE_REGEX)) {
            tokenizer.getNext();
        }
        else if (tokenizer.checkToken(DslConstants.VAR_REGEX)) {
            safeAddDeclaration(parseVariable());
        }
        else if (tokenizer.checkToken(DslConstants.LIST_REGEX)) {
            safeAddDeclaration(parseList());
        }
        else if (tokenizer.checkToken(DslConstants.PLAY_REGEX)) {
            this.statements.add(parsePlaySync());
        }
        else if (tokenizer.checkToken(DslConstants.SIMUL_REGEX)) {
            this.statements.add(parsePlaySimul());
        }
        else if (tokenizer.checkToken(DslConstants.LOOP_REGEX)) {
            this.statements.add(parseLoop());
        }
        else if (tokenizer.checkToken(DslConstants.FUNCTION_REGEX)) {
            safeAddDeclaration(parseFunction());
        }
        else if (tokenizer.checkToken(DslConstants.RHYTHM_REGEX)) {
            this.statements.add(0, parseRhythm());
        }
        else if (tokenizer.checkToken(DslConstants.START_REGEX) || tokenizer.checkToken(DslConstants.STOP_REGEX)) {
            tokenizer.getNext();
        }
        else {
            String invalidStatement = tokenizer.getNext();
            log.error("Invalid Statement in program: " + invalidStatement);
            throw new ParserException("Failed to parse. Invalid token : " + invalidStatement);
        }
    }

    private Variable parseVariable() throws ParserException {
        final String name = tokenizer.getNext().split(" ")[2].trim();
        final String[] noteArray = tokenizer.getAndCheckNext(DslConstants.NOTES_REGEX).split("\\(")[1].replace(")", "").split(",");
        final List<Note> notes = newNoteList(noteArray);
        final String instrument = tokenizer.getAndCheckNext(DslConstants.INSTRUMENT_REGEX).split("\\(|\\)")[1];
        final Integer tempo = Integer.parseInt(tokenizer.getAndCheckNext(DslConstants.TEMPO_REGEX).split("\\(|\\)")[1]);
        return new Variable(name, notes, instrument, tempo);
    }

    private DslList parseList() throws ParserException {
        String[] tokens = tokenizer.getNext().split(" |\\(|\\)");
        final String name = tokens[2].trim();
        tokens = Arrays.copyOfRange(tokens, 3, tokens.length);
        final List<Declaration> declarations = new ArrayList<>();
        for (String token : tokens) {
            declarations.add(safeGetDeclaration(token.replaceAll(",", "")));
        }
        return new DslList(name, declarations);
    }

    private PlaySimul parsePlaySimul() throws ParserException {
        return new PlaySimul(parsePlay(tokenizer.getNext().replace("PLAY SIMUL ", "")));
    }

    private PlaySync parsePlaySync() throws ParserException {
        return new PlaySync(parsePlay(tokenizer.getNext().replace("PLAY ", "")));
    }

    private List<Declaration> parsePlay(String statement) throws ParserException {
        final String[] tokens = statement.split("( )|(, )");
        final List<Declaration> declarations = new ArrayList<>();
        for (String token : tokens) {
            declarations.add(safeGetDeclaration(token));
        }
        return declarations;
    }

    private Loop parseLoop() throws ParserException {
        final int times = Integer.parseInt(tokenizer.getNext().split(" ")[1]);
        final List<Execution> executions = new ArrayList<>();
        while(!tokenizer.checkToken(DslConstants.END_REGEX)) {
            if (tokenizer.checkToken(DslConstants.NEWLINE_REGEX)) {
                tokenizer.getNext();
            }
            else if (tokenizer.checkToken(DslConstants.PLAY_REGEX)) {
                executions.add(parsePlaySync());
            }
            else if (tokenizer.checkToken(DslConstants.LOOP_REGEX)) {
                executions.add(parseLoop());
            }
            else if (tokenizer.checkToken(DslConstants.SIMUL_REGEX)) {
                executions.add(parsePlaySimul());
            }
            else {
                String msg = "Invalid Statement in loop: " + tokenizer.getNext();
                log.error(msg);
                throw new ParserException("Failed to parse program.\n" + msg);
            }
        }
        tokenizer.getNext();
        return new Loop(executions, times);
    }

    private Function parseFunction() throws ParserException {
        String name = tokenizer.getNext().split(" ")[1].trim();
        tokenizer.getAndCheckNext("\\{");
        final List<Execution> executions = new ArrayList<>();
        while (!tokenizer.checkToken("\\}")) {
            if (tokenizer.checkToken(DslConstants.NEWLINE_REGEX)) {
                tokenizer.getNext();
            }
            else if (tokenizer.checkToken(DslConstants.PLAY_REGEX)) {
                executions.add(parsePlaySync());
            }
            else if (tokenizer.checkToken(DslConstants.LOOP_REGEX)) {
                executions.add(parseLoop());
            }
            else if (tokenizer.checkToken(DslConstants.SIMUL_REGEX)) {
                executions.add(parsePlaySimul());
            }
            else {
                String msg = "Invalid statement in function" + name;
                log.error(msg);
                throw new ParserException("Failed to parse program.\n" + msg);
            }
        }
        tokenizer.getNext();
        return new Function(name, executions);
    }

    private Rhythm parseRhythm() {
        String[] tokens = tokenizer.getNext().split("( )|\\(|\\)");
        return Rhythm.builder()
                .layer1(tokens[2])
                .layer2(tokens[5])
                .layer3(tokens[8])
                .build();
    }

    private List<Note> newNoteList(String[] notes) {
        final List<Note> noteList = new ArrayList<>();
        for (String note : notes) {
            noteList.add(new Note(note));
        }
        return noteList;
    }

    private void safeAddDeclaration(Declaration declaration) throws ParserException {
        final String name = declaration.getName();
        if (this.declarationMap.containsKey(name)) {
            String msg = name + " was declared multiple times.";
            log.error(msg);
            throw new ParserException("Failed to parse program.\n" + msg);
        } else {
            this.declarationMap.put(name, declaration);
        }
    }

    private Declaration safeGetDeclaration(String name) throws ParserException {
        if (declarationMap.containsKey(name)) {
            return this.declarationMap.get(name);
        } else {
            String msg = "Attempted to add an undeclared variable to list " + name;
            log.error(msg);
            throw new ParserException("FAILED to parse program.\n" + msg);
        }
    }
}