/*
 * SeaHorseWholeTokenizer.java
 *
 * THIS FILE HAS BEEN GENERATED AUTOMATICALLY. DO NOT EDIT!
 */

package jg.sh.compile_old.parsing.parser;

import java.io.Reader;

import net.percederberg.grammatica.parser.ParserCreationException;
import net.percederberg.grammatica.parser.TokenPattern;
import net.percederberg.grammatica.parser.Tokenizer;

/**
 * A character stream tokenizer.
 *
 *
 */
public class SeaHorseWholeTokenizer extends Tokenizer {

    /**
     * Creates a new tokenizer for the specified input stream.
     *
     * @param input          the input stream to read
     *
     * @throws ParserCreationException if the tokenizer couldn't be
     *             initialized correctly
     */
    public SeaHorseWholeTokenizer(Reader input)
        throws ParserCreationException {

        super(input, false);
        createPatterns();
    }

    /**
     * Initializes the tokenizer by creating all the token patterns.
     *
     * @throws ParserCreationException if the tokenizer couldn't be
     *             initialized correctly
     */
    private void createPatterns() throws ParserCreationException {
        TokenPattern  pattern;

        pattern = new TokenPattern(SeaHorseWholeConstants.WHITESPACE,
                                   "WHITESPACE",
                                   TokenPattern.REGEXP_TYPE,
                                   "[ \\t\\n\\r]+");
        pattern.setIgnore();
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.STRING,
                                   "STRING",
                                   TokenPattern.REGEXP_TYPE,
                                   "\\\".*?\\\"");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.INTEGER,
                                   "INTEGER",
                                   TokenPattern.REGEXP_TYPE,
                                   "([0-9]+)");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.DOUBLE,
                                   "DOUBLE",
                                   TokenPattern.REGEXP_TYPE,
                                   "(\\d+\\.\\d+|\\d+\\.\\d+)");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.TRUE,
                                   "TRUE",
                                   TokenPattern.STRING_TYPE,
                                   "true");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.FALSE,
                                   "FALSE",
                                   TokenPattern.STRING_TYPE,
                                   "false");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.NULL,
                                   "NULL",
                                   TokenPattern.STRING_TYPE,
                                   "null");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.VOID,
                                   "VOID",
                                   TokenPattern.STRING_TYPE,
                                   "void");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.IS,
                                   "IS",
                                   TokenPattern.STRING_TYPE,
                                   "is");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.CONST,
                                   "CONST",
                                   TokenPattern.STRING_TYPE,
                                   "const");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.VAR,
                                   "VAR",
                                   TokenPattern.STRING_TYPE,
                                   "var");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.FUNC,
                                   "FUNC",
                                   TokenPattern.STRING_TYPE,
                                   "func");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.CONSTR,
                                   "CONSTR",
                                   TokenPattern.STRING_TYPE,
                                   "constr");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.RETURN,
                                   "RETURN",
                                   TokenPattern.STRING_TYPE,
                                   "return");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.SEAL,
                                   "SEAL",
                                   TokenPattern.STRING_TYPE,
                                   "seal");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.FOR,
                                   "FOR",
                                   TokenPattern.STRING_TYPE,
                                   "for");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.WHILE,
                                   "WHILE",
                                   TokenPattern.STRING_TYPE,
                                   "while");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.IF,
                                   "IF",
                                   TokenPattern.STRING_TYPE,
                                   "if");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.ELSE,
                                   "ELSE",
                                   TokenPattern.STRING_TYPE,
                                   "else");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.ELIF,
                                   "ELIF",
                                   TokenPattern.STRING_TYPE,
                                   "elif");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.SWITCH,
                                   "SWITCH",
                                   TokenPattern.STRING_TYPE,
                                   "switch");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.DO,
                                   "DO",
                                   TokenPattern.STRING_TYPE,
                                   "do");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.DEF,
                                   "DEF",
                                   TokenPattern.STRING_TYPE,
                                   "default");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.CASE,
                                   "CASE",
                                   TokenPattern.STRING_TYPE,
                                   "case");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.CONT,
                                   "CONT",
                                   TokenPattern.STRING_TYPE,
                                   "continue");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.BREAK,
                                   "BREAK",
                                   TokenPattern.STRING_TYPE,
                                   "break");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.TRY,
                                   "TRY",
                                   TokenPattern.STRING_TYPE,
                                   "try");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.CATCH,
                                   "CATCH",
                                   TokenPattern.STRING_TYPE,
                                   "catch");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.MODULE,
                                   "MODULE",
                                   TokenPattern.STRING_TYPE,
                                   "module");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.SELF,
                                   "SELF",
                                   TokenPattern.STRING_TYPE,
                                   "self");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.CAPTURE,
                                   "CAPTURE",
                                   TokenPattern.STRING_TYPE,
                                   "capture");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.DATA,
                                   "DATA",
                                   TokenPattern.STRING_TYPE,
                                   "data");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.USE,
                                   "USE",
                                   TokenPattern.STRING_TYPE,
                                   "use");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.FROM,
                                   "FROM",
                                   TokenPattern.STRING_TYPE,
                                   "from");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.THROW,
                                   "THROW",
                                   TokenPattern.STRING_TYPE,
                                   "throw");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.EXPORT,
                                   "EXPORT",
                                   TokenPattern.STRING_TYPE,
                                   "export");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.NAME,
                                   "NAME",
                                   TokenPattern.REGEXP_TYPE,
                                   "[a-zA-Z][a-zA-Z0-9_]*");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.ARROW,
                                   "ARROW",
                                   TokenPattern.STRING_TYPE,
                                   "->");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.PLUS,
                                   "PLUS",
                                   TokenPattern.STRING_TYPE,
                                   "+");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.MINUS,
                                   "MINUS",
                                   TokenPattern.STRING_TYPE,
                                   "-");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.MULT,
                                   "MULT",
                                   TokenPattern.STRING_TYPE,
                                   "*");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.DIV,
                                   "DIV",
                                   TokenPattern.STRING_TYPE,
                                   "/");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.MOD,
                                   "MOD",
                                   TokenPattern.STRING_TYPE,
                                   "%");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.AND,
                                   "AND",
                                   TokenPattern.STRING_TYPE,
                                   "&");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.OR,
                                   "OR",
                                   TokenPattern.STRING_TYPE,
                                   "|");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.BANG,
                                   "BANG",
                                   TokenPattern.STRING_TYPE,
                                   "!");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.COLON,
                                   "COLON",
                                   TokenPattern.STRING_TYPE,
                                   ":");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.SEMICOLON,
                                   "SEMICOLON",
                                   TokenPattern.STRING_TYPE,
                                   ";");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.LESS,
                                   "LESS",
                                   TokenPattern.STRING_TYPE,
                                   "<");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.GREAT,
                                   "GREAT",
                                   TokenPattern.STRING_TYPE,
                                   ">");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.EQUAL,
                                   "EQUAL",
                                   TokenPattern.STRING_TYPE,
                                   "=");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.EQ_EQ,
                                   "EQ_EQ",
                                   TokenPattern.STRING_TYPE,
                                   "==");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.NOT_EQ,
                                   "NOT_EQ",
                                   TokenPattern.STRING_TYPE,
                                   "!=");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.GR_EQ,
                                   "GR_EQ",
                                   TokenPattern.STRING_TYPE,
                                   ">=");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.LS_EQ,
                                   "LS_EQ",
                                   TokenPattern.STRING_TYPE,
                                   "<=");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.EQ_MULT,
                                   "EQ_MULT",
                                   TokenPattern.STRING_TYPE,
                                   "*=");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.EQ_ADD,
                                   "EQ_ADD",
                                   TokenPattern.STRING_TYPE,
                                   "+=");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.EQ_DIV,
                                   "EQ_DIV",
                                   TokenPattern.STRING_TYPE,
                                   "/=");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.EQ_MIN,
                                   "EQ_MIN",
                                   TokenPattern.STRING_TYPE,
                                   "-=");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.EQ_MOD,
                                   "EQ_MOD",
                                   TokenPattern.STRING_TYPE,
                                   "%=");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.BOOL_AND,
                                   "BOOL_AND",
                                   TokenPattern.STRING_TYPE,
                                   "&&");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.BOOL_OR,
                                   "BOOL_OR",
                                   TokenPattern.STRING_TYPE,
                                   "||");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.OP_PAREN,
                                   "OP_PAREN",
                                   TokenPattern.STRING_TYPE,
                                   "(");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.CL_PAREN,
                                   "CL_PAREN",
                                   TokenPattern.STRING_TYPE,
                                   ")");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.COMMA,
                                   "COMMA",
                                   TokenPattern.STRING_TYPE,
                                   ",");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.DOT,
                                   "DOT",
                                   TokenPattern.STRING_TYPE,
                                   ".");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.OP_SQ_BRACK,
                                   "OP_SQ_BRACK",
                                   TokenPattern.STRING_TYPE,
                                   "[");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.CL_SQ_BRACK,
                                   "CL_SQ_BRACK",
                                   TokenPattern.STRING_TYPE,
                                   "]");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.OP_CU_BRACK,
                                   "OP_CU_BRACK",
                                   TokenPattern.STRING_TYPE,
                                   "{");
        addPattern(pattern);

        pattern = new TokenPattern(SeaHorseWholeConstants.CL_CU_BRACK,
                                   "CL_CU_BRACK",
                                   TokenPattern.STRING_TYPE,
                                   "}");
        addPattern(pattern);
    }
}
