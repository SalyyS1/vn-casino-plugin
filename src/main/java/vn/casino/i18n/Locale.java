package vn.casino.i18n;

public enum Locale {
    VI("vi", "Tiếng Việt"),
    EN("en", "English");

    private final String code;
    private final String displayName;

    Locale(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Locale fromCode(String code) {
        for (Locale locale : values()) {
            if (locale.code.equalsIgnoreCase(code)) {
                return locale;
            }
        }
        return VI;
    }

    public static Locale fromString(String str) {
        if (str == null || str.isEmpty()) {
            return VI;
        }

        for (Locale locale : values()) {
            if (locale.name().equalsIgnoreCase(str) || locale.code.equalsIgnoreCase(str)) {
                return locale;
            }
        }
        return VI;
    }

    @Override
    public String toString() {
        return code;
    }
}
