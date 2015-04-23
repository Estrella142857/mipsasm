/*
 * Copyright (c) 2015 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.mipsasm.gui;

import me.zhanghai.mipsasm.assembler.DirectiveInformation;
import me.zhanghai.mipsasm.assembler.OperationInformation;
import me.zhanghai.mipsasm.assembler.Register;
import me.zhanghai.mipsasm.parser.Tokens;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StyledTextStyleHelper {

    private enum StyleType {
        ERROR,
        COMMENT,
        PUNCTUATION,
        KEYWORD,
        REGISTER,
        IMMEDIATE
    }

    private static final EnumMap<StyleType, StyleRangeTemplate> THEME_MONOKAI;
    static {
        THEME_MONOKAI = new EnumMap<>(StyleType.class);
        THEME_MONOKAI.put(StyleType.ERROR, new StyleRangeTemplate(
                new StyleRangeTemplate.UnderlineStyle(new Color(Display.getCurrent(), 0xF9, 0x26, 0x72),
                        SWT.UNDERLINE_SQUIGGLE)));
        THEME_MONOKAI.put(StyleType.COMMENT, new StyleRangeTemplate(new Color(Display.getCurrent(), 0x75, 0x71, 0x51)));
        THEME_MONOKAI.put(StyleType.PUNCTUATION, new StyleRangeTemplate(
                new Color(Display.getCurrent(), 0xF9, 0x26, 0x72)));
        THEME_MONOKAI.put(StyleType.KEYWORD, new StyleRangeTemplate(new Color(Display.getCurrent(), 0xA6, 0xE2, 0x2E),
                SWT.BOLD));
        THEME_MONOKAI.put(StyleType.REGISTER, new StyleRangeTemplate(
                new Color(Display.getCurrent(), 0xFD, 0x97, 0x1F)));
        THEME_MONOKAI.put(StyleType.IMMEDIATE, new StyleRangeTemplate(
                new Color(Display.getCurrent(), 0xAE, 0x81, 0xFF)));
    }

    private static final EnumMap<StyleType, StyleRangeTemplate> THEME_SOLARIZED;
    static {
        Display display = Display.getCurrent();
        Color BASE03 = new Color(display, 0x00, 0x2b, 0x36);
        Color BASE02 = new Color(display, 0x07, 0x36, 0x42);
        Color BASE01 = new Color(display, 0x58, 0x6E, 0x75);
        Color BASE00 = new Color(display, 0x65, 0x7B, 0x83);
        Color BASE0 = new Color(display, 0x83, 0x94, 0x96);
        Color BASE1 = new Color(display, 0x93, 0xA1, 0xA1);
        Color BASE2 = new Color(display, 0xEE, 0xE8, 0xD5);
        Color BASE3 = new Color(display, 0xFD, 0xF6, 0xE3);
        Color YELLOW = new Color(display, 0xB5, 0x89, 0x00);
        Color ORANGE = new Color(display, 0xCB, 0x4B, 0x16);
        Color RED = new Color(display, 0xDC, 0x32, 0x2F);
        Color MAGENTA = new Color(display, 0xD3, 0x36, 0x82);
        Color VIOLET = new Color(display, 0x6C, 0x71, 0xC4);
        Color BLUE = new Color(display, 0x26, 0x8B, 0xD2);
        Color CYAN = new Color(display, 0x2A, 0xA1, 0x98);
        Color GREEN = new Color(display, 0x85, 0x99, 0x00);
        THEME_SOLARIZED = new EnumMap<>(StyleType.class);
        THEME_SOLARIZED.put(StyleType.ERROR, new StyleRangeTemplate(
                new StyleRangeTemplate.UnderlineStyle(RED, SWT.UNDERLINE_SQUIGGLE)));
        THEME_SOLARIZED.put(StyleType.COMMENT, new StyleRangeTemplate(BASE1));
        THEME_SOLARIZED.put(StyleType.PUNCTUATION, new StyleRangeTemplate(MAGENTA));
        THEME_SOLARIZED.put(StyleType.KEYWORD, new StyleRangeTemplate(CYAN));
        THEME_SOLARIZED.put(StyleType.REGISTER, new StyleRangeTemplate(BLUE));
        THEME_SOLARIZED.put(StyleType.IMMEDIATE, new StyleRangeTemplate(ORANGE));
    }

    private static final String[] PUNCTUATION_REGEXS = new String [] {
            ":",
            ",",
            "\\(",
            "\\)"
    };

    private static final String TOKEN_SEPARATOR_REGEX;
    static {
        StringBuilder builder = new StringBuilder();
        for (String punctuationRegex : PUNCTUATION_REGEXS) {
            builder.append("(\\s*")
                    .append(punctuationRegex)
                    .append("\\s*)|");
        }
        builder.append("(\\s+)");
        TOKEN_SEPARATOR_REGEX = builder.toString();
    }

    private static final List<String> KEYWORD;
    static {
        KEYWORD = new ArrayList<>();
        for (OperationInformation operationInformation : OperationInformation.values()) {
            KEYWORD.add(operationInformation.name());
        }
        for (DirectiveInformation directiveInformation : DirectiveInformation.values()) {
            KEYWORD.add("." + directiveInformation.name());
        }
    }

    private static final List<String> REGISTERS;
    static {
        REGISTERS = new ArrayList<>();
        for (Register register : Register.values()) {
            REGISTERS.add("$" + register.ordinal());
            REGISTERS.add("$" + register.name());
        }
    }

    private static boolean isDarkTheme() {
        Color widgetBackground = Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        int meanComponent = (widgetBackground.getRed() + widgetBackground.getGreen() + widgetBackground.getBlue()) / 3;
        return meanComponent < 0x7F;
    }

    public static void setup(final StyledText styledText) {
        final Map<StyleType, StyleRangeTemplate> theme = isDarkTheme() ? THEME_MONOKAI : THEME_SOLARIZED;
        styledText.addLineStyleListener(new LineStyleListener() {
            @Override
            public void lineGetStyle(LineStyleEvent event) {
                setStyleRangeListForEvent(event, styledText, theme);
            }
        });
    }

    private static void setStyleRangeListForEvent(LineStyleEvent event, StyledText styledText,
                                                  Map<StyleType, StyleRangeTemplate> theme) {

        List<StyleRange> styleRangeList = new ArrayList<>();

        // Workaround case for String.indexOf().
        String line = event.lineText.toUpperCase();

        Matcher trailingSpaceMatcher = Pattern.compile("\\s+$").matcher(line);
        if (trailingSpaceMatcher.find()) {
            styleRangeList.add(theme.get(StyleType.ERROR).forRange(event.lineOffset + trailingSpaceMatcher.start(),
                    trailingSpaceMatcher.end() - trailingSpaceMatcher.start()));
            line = line.substring(0, trailingSpaceMatcher.start());
        }

        Matcher commentMatcher = Pattern.compile(Tokens.COMMENT_REGEX).matcher(line);
        if (commentMatcher.find()) {
            styleRangeList.add(theme.get(StyleType.COMMENT).forRange(event.lineOffset + commentMatcher.start(),
                    commentMatcher.end() - commentMatcher.start()));
            line = line.substring(0, commentMatcher.start());
        }

        Matcher statementSeparatorMatcher = Pattern.compile(Tokens.STATEMENT_SEPARATOR_REGEX).matcher(line);
        int statementStart = 0;
        while (true) {
            boolean statementSeparatorFound = statementSeparatorMatcher.find();
            String statement;
            if (statementSeparatorFound) {
                statement = line.substring(statementStart, statementSeparatorMatcher.start());
            } else {
                statement = line.substring(statementStart, line.length());
            }
            addStyleForStatement(statement, event.lineOffset + statementStart, styleRangeList, theme);
            if (statementSeparatorFound) {
                styleRangeList.add(theme.get(StyleType.PUNCTUATION).forRange(
                        event.lineOffset + statementSeparatorMatcher.start(),
                        statementSeparatorMatcher.end() - statementSeparatorMatcher.start()));
                statementStart = statementSeparatorMatcher.end();
            } else {
                break;
            }
        }

        // StyleRange[] must be sorted, according to documentation, or behavior will be weird.
        sortStyleRangeList(styleRangeList);

        event.styles = new StyleRange[styleRangeList.size()];
        styleRangeList.toArray(event.styles);
    }

    private static void addStyleForStatement(String statement, int offset, List<StyleRange> styleRangeList,
                                             Map<StyleType, StyleRangeTemplate> theme) {
        Matcher tokenSeparatorMatcher = Pattern.compile(TOKEN_SEPARATOR_REGEX).matcher(statement);
        int tokenStart = 0;
        while (true) {
            boolean tokenSeparatorFound = tokenSeparatorMatcher.find();
            String token;
            if (tokenSeparatorFound) {
                token = statement.substring(tokenStart, tokenSeparatorMatcher.start());
            } else {
                token = statement.substring(tokenStart, statement.length());
            }
            addStyleForToken(token, offset + tokenStart, styleRangeList, theme);
            if (tokenSeparatorFound) {
                addStyleForPunctuation(tokenSeparatorMatcher.group(), offset + tokenSeparatorMatcher.start(),
                        styleRangeList, theme);
                tokenStart = tokenSeparatorMatcher.end();
            } else {
                break;
            }
        }
    }

    private static void addStyleForToken(String token, int offset, List<StyleRange> styleRangeList,
                                         Map<StyleType, StyleRangeTemplate> theme) {

        for (String keyword : KEYWORD) {
            if (token.equals(keyword)) {
                styleRangeList.add(theme.get(StyleType.KEYWORD).forRange(offset, token.length()));
            }
        }

        for (String register : REGISTERS) {
            if (token.equals(register)) {
                styleRangeList.add(theme.get(StyleType.REGISTER).forRange(offset, token.length()));
                return;
            }
        }

        Matcher immediateMatcher = Pattern.compile("(0X[0-9A-F]{1,8})|(0[0-7]{1,11})|(0B[01]{1,32})|(\\d{1,10})")
                .matcher(token);
        if (immediateMatcher.matches()) {
            styleRangeList.add(theme.get(StyleType.IMMEDIATE).forRange(offset, token.length()));
        }
    }

    private static void addStyleForPunctuation(String tokenSeparator, int offset, List<StyleRange> styleRangeList,
                                               Map<StyleType, StyleRangeTemplate> theme) {
        for (String punctuationRegex : PUNCTUATION_REGEXS) {
            Matcher punctuationMatcher = Pattern.compile(punctuationRegex).matcher(tokenSeparator);
            while (punctuationMatcher.find()) {
                styleRangeList.add(theme.get(StyleType.PUNCTUATION).forRange(offset + punctuationMatcher.start(),
                        punctuationMatcher.end() - punctuationMatcher.start()));
            }
        }
    }

    private static void sortStyleRangeList(List<StyleRange> styleRangeList) {
        Collections.sort(styleRangeList, new Comparator<StyleRange>() {
            @Override
            public int compare(StyleRange left, StyleRange right) {
                int result = left.start - right.start;
                if (result == 0) {
                    result = left.length - right.length;
                }
                return result;
            }
        });
    }

    private static class StyleRangeTemplate {

        private Color foreground;
        private Color background;
        private int fontStyle;
        private UnderlineStyle underlineStyle;

        public StyleRangeTemplate(Color foreground, Color background, int fontStyle, UnderlineStyle underlineStyle) {
            this.foreground = foreground;
            this.background = background;
            this.fontStyle = fontStyle;
            this.underlineStyle = underlineStyle;
        }

        public StyleRangeTemplate(UnderlineStyle underlineStyle) {
            this(null, null, SWT.NORMAL, underlineStyle);
        }

        public StyleRangeTemplate(Color foreground, int fontStyle) {
            this(foreground, null, fontStyle, new UnderlineStyle());
        }

        public StyleRangeTemplate(Color foreground) {
            this(foreground, SWT.NORMAL);
        }

        public StyleRange forRange(int start, int length) {
            StyleRange styleRange = new StyleRange(start, length, foreground, background, fontStyle);
            underlineStyle.applyTo(styleRange);
            return styleRange;
        }

        private static class UnderlineStyle {
            private boolean underline;
            private Color color;
            private int style;

            private UnderlineStyle(boolean underline, Color color, int style) {
                this.underline = underline;
                this.color = color;
                this.style = style;
            }

            public UnderlineStyle() {
                this(false, null, SWT.SINGLE);
            }

            public UnderlineStyle(Color color, int style) {
                this(true, color, style);
            }

            public UnderlineStyle(Color color) {
                this(color, SWT.SINGLE);
            }

            public void applyTo(StyleRange styleRange) {
                styleRange.underline = underline;
                styleRange.underlineColor = color;
                styleRange.underlineStyle = style;
            }
        }
    }
}
