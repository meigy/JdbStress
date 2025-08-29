package com.meigy.jstress.core;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.*;
import java.util.stream.Collectors;

/*
定义:pt表示用当前一个时间戳生成一个长整数，并用这个数替换掉字符串中所有的:pt;
定义:ps表示用当前一个时间戳生成一个字符串并拼接一个随机数，并用这个字符串替换掉字符串中所有的:ps;
定义:pr表示生成一个随机的正整数，执行结果是生成一个正整数，并用这个数替换掉字符串中所有的:pr;
定义:PR表示生成一个随机的正整数，执行结果是生成一个正整数，并用这个数替换掉字符串中一个:PR，如何后面还有:PR需再生成一个随机数替换下一个:PR;
定义:pi[100]表示用一个自增数发生器（从100开始的整数发生器）的下一个值，替换掉字符串中所有的:pi[100]
定义:PI[100]表示用一个自增数发生器（从100开始的整数发生器）的下一个值，替换掉字符串中一个:PI[100]，如何后面还有:PI[100]需再取下一个值替换下一个PI[100];
定义:pl[1000]表示用一个自增数发生器（从1000开始的长整数发生器）的下一个值，替换掉字符串中所有的:pl[1000]
定义:PL[1000]表示用一个自增数发生器（从1000开始的长整数发生器）的下一个值，替换掉字符串中一个:PL[1000]，如何后面还有:PL[1000]需再取下一个值替换下一个PL[1000];
 */
public class CustomStringParser {
    private final String inputTemplate;
    private final List<Fragment> fragments;
    //private static final Random random = new Random();
    //private static final Map<String, AtomicInteger> intCounters = new HashMap<>();
    //private static final Map<String, AtomicLong> longCounters = new HashMap<>();

    private static final String EMPTY_STRING = "";

    private static final List<VariantParser> ALL_DEFAULT_PATTERNS = Arrays.asList(
            TimeLongParser.DEFAULT,
            RandomStringParser.DEFAULT,
            RandomParser.DEFAULT,
            SequentialRandomParser.DEFAULT,
            IntIncrementParser.DEFAULT,
            LongIncrementParser.DEFAULT,
            SequentialIntIncrementParser.DEFAULT,
            SequentialLongIncrementParser.DEFAULT
    );
    private static final Map<Class<? extends VariantParser>, VariantParser> PARSERS_DEFINEDS = ALL_DEFAULT_PATTERNS.stream().collect(Collectors.toMap(VariantParser::getClass, p -> p));
    private static final List<String> ALL_PATTERNS = ALL_DEFAULT_PATTERNS.stream()
            .map(VariantParser::getPattern)
            .collect(Collectors.toList());
    private static final Pattern COMBINED_PATTERN = Pattern.compile(
            ALL_PATTERNS.stream()
                    .reduce((a, b) -> a + "|" + b)
                    .orElse("")
    );

    private static final Object lock = new Object();

    // 片段类型：静态文本或动态解析器
    private static abstract class Fragment {
        abstract String getNextValue();
        abstract void reset();
    }

    // 静态文本片段
    private static class StaticFragment extends Fragment {
        private final String text;

        StaticFragment(String text) {
            this.text = text;
        }

        @Override
        String getNextValue() {
            return text;
        }

        @Override
        void reset() {
            // No state to reset for static fragments
        }
    }

    // 动态解析器片段
    private static class DynamicFragment extends Fragment {
        private final VariantParser parser;

        DynamicFragment(VariantParser parser) {
            this.parser = parser;
        }

        @Override
        String getNextValue() {
            return parser.getNext();
        }

        @Override
        void reset() {
            parser.reset();
        }
    }

    public CustomStringParser(String input) {
        this.inputTemplate = input;
        this.fragments = parseFragments();
    }

    private List<Fragment> parseFragments() {
        List<Fragment> result = new ArrayList<>();
        Matcher matcher = COMBINED_PATTERN.matcher(inputTemplate);
        Map<String, VariantParser> parserCache = new HashMap<>();
        int lastEnd = 0;

        while (matcher.find()) {
            // 添加静态文本
            if (matcher.start() > lastEnd) {
                result.add(new StaticFragment(inputTemplate.substring(lastEnd, matcher.start())));
            }

            // 处理动态标记
            String token = matcher.group();
            if (!parserCache.containsKey(token)) {
                VariantParser parser = createParser(token);
                parserCache.put(token, parser);
            }
            VariantParser parser = parserCache.get(token);
            result.add(new DynamicFragment(parser));

            lastEnd = matcher.end();
        }

        // 添加末尾静态文本
        if (lastEnd < inputTemplate.length()) {
            result.add(new StaticFragment(inputTemplate.substring(lastEnd)));
        }

        return result;
    }

    private VariantParser createParser(String token) {
        for (VariantParser parser : ALL_DEFAULT_PATTERNS) {
            //Pattern pattern = Pattern.compile(parser.getPattern().replace("[", "\\[").replace("]", "\\]"));
            Pattern pattern = Pattern.compile(parser.getPattern());
            Matcher matcher = pattern.matcher(token);
            if (matcher.matches()) {
                return parser.createInstance(token);
            }
        }
        throw new IllegalArgumentException("No parser found for token: " + token);
    }

    public String getNext() {
        synchronized (lock) {
            for (Fragment fragment : fragments) {
                fragment.reset();
            }

            StringBuilder result = new StringBuilder();
            for (Fragment fragment : fragments) {
                result.append(fragment.getNextValue());
            }
            return result.toString();
        }
    }

    // 解析器接口
    private interface VariantParser {
        String getNext();
        String getPattern();
        void reset();
        VariantParser createInstance(String token);
    }

    // 时间戳解析器（长整数）
    private static class TimeLongParser implements VariantParser {

        public static final TimeLongParser DEFAULT = new TimeLongParser();
        private String lastValue = EMPTY_STRING;
        @Override
        public String getNext() {
            if (lastValue.isEmpty()) {
                lastValue = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            }
            return lastValue;
        }

        @Override
        public String getPattern() {
            return ":pt";
        }

        @Override
        public void reset() {
            lastValue = EMPTY_STRING;
        }

        @Override
        public VariantParser createInstance(String token) {
            return new TimeLongParser();
        }
    }

    // 时间戳+随机数解析器
    private static class RandomStringParser implements VariantParser {
        public static final RandomStringParser DEFAULT = new RandomStringParser();

        private String lastValue = EMPTY_STRING;
        private final Random random = new Random();
        @Override
        public String getNext() {
            if (lastValue.isEmpty()) {
                String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                int randomNum = 1000000 + random.nextInt(9000000);
                lastValue = timestamp + randomNum;
            }
            return lastValue;
        }

        @Override
        public String getPattern() {
            return ":ps";
        }

        @Override
        public void reset() {
            lastValue = EMPTY_STRING;
        }

        @Override
        public VariantParser createInstance(String token) {
            return new RandomStringParser();
        }
    }

    // 一次随机数解析器
    private static class RandomParser implements VariantParser {
        public static final RandomParser DEFAULT = new RandomParser();
        private String lastValue = EMPTY_STRING;
        private final Random random = new Random();
        @Override
        public String getNext() {
            if (lastValue.isEmpty()) {
                lastValue = String.valueOf(random.nextInt(Integer.MAX_VALUE) + 1);
            }
            return lastValue;
        }

        @Override
        public String getPattern() {
            return ":pr";
        }

        @Override
        public void reset() {
            lastValue = EMPTY_STRING;
        }

        @Override
        public VariantParser createInstance(String token) {
            return new RandomParser();
        }
    }

    // 每次随机数解析器
    private static class SequentialRandomParser implements VariantParser {
        public static final SequentialRandomParser DEFAULT = new SequentialRandomParser();
        private final Random random = new Random();
        @Override
        public String getNext() {
            return String.valueOf(random.nextInt(Integer.MAX_VALUE) + 1);
        }

        @Override
        public String getPattern() {
            return ":PR";
        }

        @Override
        public void reset() {

        }

        @Override
        public VariantParser createInstance(String token) {
            return new SequentialRandomParser();
        }
    }

    // 一次自增整数解析器
    private static class IntIncrementParser implements VariantParser {
        public static final IntIncrementParser DEFAULT = new IntIncrementParser(":pi[0]");
        private String lastValue = EMPTY_STRING;
        private final AtomicInteger counter;

        IntIncrementParser(String token) {
            //从payload :pi\[\d+\] 中提取起始值
            String numberStr = token.substring(token.indexOf('[') + 1, token.indexOf(']'));
            int start = Integer.parseInt(numberStr);
            counter = new AtomicInteger(start + 1);
        }

        @Override
        public String getNext() {
            if (lastValue.isEmpty()) {
                counter.getAndIncrement();
                lastValue = String.valueOf(counter);
            }
            return lastValue;
        }

        @Override
        public String getPattern() {
            return ":pi\\[\\d+\\]";
        }

        @Override
        public void reset() {
            lastValue = EMPTY_STRING;
        }

        @Override
        public VariantParser createInstance(String token) {
            return new IntIncrementParser(token);
        }
    }

    // 每次自增整数解析器
    private static class SequentialIntIncrementParser implements VariantParser {
        public static final SequentialIntIncrementParser DEFAULT = new SequentialIntIncrementParser(":PI[0]");
        private final AtomicInteger counter;

        SequentialIntIncrementParser(String token) {
            //从payload :pi\[\d+\] 中提取起始值
            String numberStr = token.substring(token.indexOf('[') + 1, token.indexOf(']'));
            int start = Integer.parseInt(numberStr);
            counter = new AtomicInteger(start + 1);
        }

        @Override
        public String getNext() {
            return String.valueOf(counter.getAndIncrement());
        }

        @Override
        public String getPattern() {
            return ":PI\\[\\d+\\]";
        }

        @Override
        public void reset() {

        }

        @Override
        public VariantParser createInstance(String token) {
            return new SequentialIntIncrementParser(token);
        }
    }

    // 一次自增长整数解析器
    private static class LongIncrementParser implements VariantParser {
        public static final LongIncrementParser DEFAULT = new LongIncrementParser(":pl[0]");
        private String lastValue = EMPTY_STRING;
        private final AtomicLong counter;

        LongIncrementParser(String token) {
            //从payload :pi\[\d+\] 中提取起始值
            String numberStr = token.substring(token.indexOf('[') + 1, token.indexOf(']'));
            long start = Long.parseLong(numberStr);
            counter = new AtomicLong(start + 1);
        }

        @Override
        public String getNext() {
            if (lastValue.isEmpty()) {
                counter.getAndIncrement();
                lastValue = String.valueOf(counter);
            }
            return lastValue;
        }

        @Override
        public String getPattern() {
            return ":pl\\[\\d+\\]";
        }

        @Override
        public void reset() {
            lastValue = EMPTY_STRING;
        }

        @Override
        public VariantParser createInstance(String token) {
            return new LongIncrementParser(token);
        }
    }

    // 顺序自增长整数解析器
    private static class SequentialLongIncrementParser implements VariantParser {
        public static final SequentialLongIncrementParser DEFAULT = new SequentialLongIncrementParser(":PL[0]");
        private final AtomicLong counter;

        SequentialLongIncrementParser(String token) {
            //从payload :pi\[\d+\] 中提取起始值
            String numberStr = token.substring(token.indexOf('[') + 1, token.indexOf(']'));
            long start = Long.parseLong(numberStr);
            counter = new AtomicLong(start + 1);
        }

        @Override
        public String getNext() {
            return String.valueOf(counter.getAndIncrement());
        }

        @Override
        public String getPattern() {
            return ":PL\\[\\d+\\]";
        }

        @Override
        public void reset() {

        }

        @Override
        public VariantParser createInstance(String token) {
            return new SequentialLongIncrementParser(token);
        }
    }
}