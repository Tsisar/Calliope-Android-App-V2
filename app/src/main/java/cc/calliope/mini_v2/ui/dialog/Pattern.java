package cc.calliope.mini_v2.ui.dialog;

import java.util.LinkedHashMap;
import java.util.Map;

public enum Pattern {
        XX(0f),
        ZU(1f),
        VO(2f),
        GI(3f),
        PE(4f),
        TA(5f);

        private final float code;

        Pattern(final float code) {
            this.code = code;
        }

        private static final Map<Float, Pattern> BY_CODE_MAP = new LinkedHashMap<>();

        static {
            for (Pattern pattern : Pattern.values()) {
                BY_CODE_MAP.put(pattern.code, pattern);
            }
        }

        public static Pattern forCode(float code) {
            return BY_CODE_MAP.get(code);
        }
    }