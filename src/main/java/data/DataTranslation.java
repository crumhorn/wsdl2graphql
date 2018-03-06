package data;

public class DataTranslation {

    public static final String KEY_NO_ARGUMENTS = "noArguments";
    public static final String KEY_WITH_ARGUMENTS = "withArguments";

    public static final String REPLACE_VAL_QUERY_NAME = "%QUERY_NAME%";
    public static final String IS_LIST_RETURN = "%IS_LIST_RETURN%";
    public static final String QUERY_ARGS = "%QUERY_ARGS%";

    public String key;
    public String statement;

    public DataTranslation(String line) {
        if (line.indexOf(":") == -1) {
            return;
        }
        String [] splt = line.split(":");
        if (splt.length == 2) {
            this.key = splt[0];
            this.statement = splt[1];
        }
    }

    @Override
    public String toString() {
        return "[DataTranslation, key: " + this.key + " = " + this.statement + "]";
    }
}
