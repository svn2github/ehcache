/**
 * Copyright Terracotta, Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package net.sf.ehcache.search.parser;

import java.util.Date;

import net.sf.ehcache.search.parser.CustomParseException.Message;

/**
 * The model value class.
 *
 * @param <T> the generic type
 */
public abstract class MValue<T> implements ModelElement<T> {

    /**
     * The value.
     */
    private final String value;

    /**
     * The type name.
     */
    private final String typeName;

    private final Token tok;

    private final Message errMessage;

    private T javaObject;

    /**
     * Instantiates a new m value.
     *
     * @param typeName the type name
     * @param value    the value
     */
    public MValue(Token tok, String typeName, Message errMessage, String value) {
        this.value = value;
        this.errMessage = errMessage;
        this.typeName = typeName;
        this.tok = tok;
    }

    public T asEhcacheObject(ClassLoader loader) {
        return javaObject;
    }

    protected void cacheJavaObject() throws CustomParseException {
        try {
            javaObject = constructJavaObject();
        } catch (Throwable e) {
            throw CustomParseException.factory(tok, errMessage);
        }
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "(" + typeName + ")'" + (value == null ? "null" : value) + "'";
    }

    /**
     * Gets the type name.
     *
     * @return the type name
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * As java object.
     *
     * @return the t
     */
    protected abstract T constructJavaObject();

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        @SuppressWarnings("rawtypes")
        MValue other = (MValue)obj;
        if (typeName == null) {
            if (other.typeName != null) return false;
        } else if (!typeName.equals(other.typeName)) return false;
        if (value == null) {
            if (other.value != null) return false;
        } else if (!value.equals(other.value)) return false;
        return true;
    }

    /**
     * The model byte class
     */
    public static class MByte extends MValue<java.lang.Byte> {

        /**
         * Instantiates a new m byte.
         *
         * @param value the value
         * @throws CustomParseException
         */
        public MByte(Token tok, String value) throws CustomParseException {
            super(tok, "byte", Message.BYTE_CAST, value);
            cacheJavaObject();
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MValue#asJavaObject()
         */
        @Override
        protected Byte constructJavaObject() {
            return (byte)Integer.parseInt(getValue());
        }
    }

    /**
     * The model boolean class
     */
    public static class MBool extends MValue<Boolean> {

        /**
         * Instantiates a new bool.
         *
         * @param value the value
         * @throws CustomParseException
         */
        public MBool(Token t, String value) throws CustomParseException {
            super(t, "bool", Message.BOOLEAN_CAST, value);
            cacheJavaObject();
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MValue#asJavaObject()
         */
        @Override
        protected Boolean constructJavaObject() {
            return Boolean.parseBoolean(getValue());
        }
    }

    /**
     * The model short class
     */
    public static class MShort extends MValue<Short> {

        /**
         * Instantiates a new m short.
         *
         * @param value the value
         * @throws CustomParseException
         */
        public MShort(Token tok, String value) throws CustomParseException {
            super(tok, "short", Message.SHORT_LITERAL, value);
            cacheJavaObject();
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MValue#asJavaObject()
         */
        @Override
        protected Short constructJavaObject() {
            return (short)Integer.parseInt(getValue());
        }
    }

    /**
     * The model integer class
     */
    public static class MInt extends MValue<Integer> {

        /**
         * Instantiates a new m int.
         *
         * @param value the value
         * @throws CustomParseException
         */
        public MInt(Token tok, String value) throws CustomParseException {
            super(tok, "int", Message.INT_LITERAL, value);
            cacheJavaObject();
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MValue#asJavaObject()
         */
        @Override
        protected Integer constructJavaObject() {
            return Integer.parseInt(getValue());
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MValue#toString()
         */
        @Override
        public String toString() {
            return constructJavaObject().toString();
        }
    }

    /**
     * The model long class
     */
    public static class MLong extends MValue<Long> {

        /**
         * Instantiates a new m long.
         *
         * @param value the value
         * @throws CustomParseException
         */
        public MLong(Token tok, String value) throws CustomParseException {
            super(tok, "long", Message.LONG_LITERAL, value);
            cacheJavaObject();
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MValue#asJavaObject()
         */
        @Override
        protected Long constructJavaObject() {
            return Long.parseLong(getValue());
        }
    }

    public static class MFloat extends MValue<Float> {

        /**
         * Instantiates a new m double.
         *
         * @param value the value
         * @throws CustomParseException
         */
        public MFloat(Token tok, String value) throws CustomParseException {
            super(tok, "float", Message.FLOAT_LITERAL, value);
            cacheJavaObject();
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MValue#asJavaObject()
         */
        @Override
        protected Float constructJavaObject() {
            return Float.parseFloat(getValue());
        }
    }
    
    /**
     * The model double class
     */
    public static class MDouble extends MValue<Double> {

        /**
         * Instantiates a new m double.
         *
         * @param value the value
         * @throws CustomParseException
         */
        public MDouble(Token tok, String value) throws CustomParseException {
            super(tok, "double", Message.DOUBLE_LITERAL, value);
            cacheJavaObject();
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MValue#asJavaObject()
         */
        @Override
        protected Double constructJavaObject() {
            return Double.parseDouble(getValue());
        }
    }

    /**
     * The model class for java dates
     */
    public static class MJavaDate extends MValue<java.util.Date> {

        /**
         * Instantiates a new m java date.
         *
         * @param value the value
         * @throws CustomParseException
         */
        public MJavaDate(Token tok, String value) throws CustomParseException {
            super(tok, "date", Message.DATE_LITERAL, value);
            cacheJavaObject();
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MValue#asJavaObject()
         */
        @Override
        protected Date constructJavaObject() {
            try {
                return ParserSupport.variantDateParse(getValue());
            } catch (net.sf.ehcache.search.parser.ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * The model class for query dates
     */
    public static class MSqlDate extends MValue<java.sql.Date> {

        /**
         * Instantiates a new m query date.
         *
         * @param value the value
         * @throws CustomParseException
         */
        public MSqlDate(Token tok, String value) throws CustomParseException {
            super(tok, "sqldate", Message.SQLDATE_LITERAL, value);
            cacheJavaObject();
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MValue#asJavaObject()
         */
        @Override
        protected java.sql.Date constructJavaObject() {
            try {
                java.util.Date d = ParserSupport.variantDateParse(getValue());
                return new java.sql.Date(d.getTime());
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * The model class for string values
     */
    public static class MString extends MValue<String> {

        /**
         * Instantiates a new m string.
         *
         * @param value the value
         * @throws CustomParseException
         */
        public MString(Token tok, String value) throws CustomParseException {
            super(tok, "string", Message.STRING_LITERAL, value);
            cacheJavaObject();
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MValue#asJavaObject()
         */
        @Override
        protected String constructJavaObject() {
            return getValue();
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MValue#toString()
         */
        @Override
        public String toString() {
            return "'" + getValue() + "'";
        }
    }

    /**
     * The model class for string values
     */
    public static class MChar extends MValue<Character> {

        /**
         * Instantiates a new m string.
         *
         * @param value the value
         * @throws CustomParseException
         */
        public MChar(Token tok, String value) throws CustomParseException {
            super(tok, "character", Message.CHAR_LITERAL, value);
            cacheJavaObject();
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MValue#asJavaObject()
         */
        @Override
        protected Character constructJavaObject() {
            return getValue().toCharArray()[0];
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MValue#toString()
         */
        @Override
        public String toString() {
            return "'" + getValue() + "'";
        }
    }

    /**
     * The class for java enum instances
     */
    public static class MEnum<T extends Enum<T>> extends MValue<Enum<T>> {

        /**
         * The class name.
         */
        private final String className;

        /**
         * Instantiates a new m enum.
         *
         * @param className the class name
         * @param value     the value
         * @throws CustomParseException
         */
        public MEnum(Token tok, String className, String value) throws CustomParseException {
            super(tok, "enum", Message.ENUM_LITERAL, value);
            this.className = className;
        }

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MValue#asJavaObject()
         */
        @Override
        protected Enum<T> constructJavaObject() {
        	throw new UnsupportedOperationException();
        }
        
        @Override
        public Enum<T> asEhcacheObject(ClassLoader loader) {
            return ParserSupport.makeEnumFromString(loader, className, getValue());
        }
        

        /*
         * (non-Javadoc)
         * @see net.sf.ehcache.search.parser.MValue#toString()
         */
        @Override
        public String toString() {
            return "(enum " + className + ")'" + getValue() + "'";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((className == null) ? 0 : className.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!super.equals(obj)) return false;
            if (getClass() != obj.getClass()) return false;
            MEnum<T> other = (MEnum<T>)obj;
            if (className == null) {
                if (other.className != null) return false;
            } else if (!className.equals(other.className)) return false;
            return true;
        }
    }


}
