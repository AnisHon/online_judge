package com.anishan.commons.e;

public class ValidationGroup {

    public interface Update {}
    public interface Delete {}
    public interface Insert {}
    public interface Select {}

    public static final Class<Update> UPDATE = Update.class;
    public static final Class<Delete> DELETE = Delete.class;
    public static final Class<Insert> INSERT = Insert.class;
    public static final Class<Select> SELECT = Select.class;


}
