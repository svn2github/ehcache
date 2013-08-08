/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.sf.ehcache.search.parser;


/**
 * The Class InteractiveCmd.
 */
public class InteractiveCmd {

    /**
     * The Enum Cmd.
     */
    public static enum Cmd {

        /**
         * Select.
         */
        Select,
        /**
         * Use cache.
         */
        UseCache,
        /**
         * Use cache manager.
         */
        UseCacheManager
    }

    /**
     * The type.
     */
    private Cmd type;

    /**
     * The qmodel.
     */
    private ParseModel qmodel;

    /**
     * The ident.
     */
    private String ident;
    ;

    /**
     * Instantiates a new interactive cmd for select.
     *
     * @param qmodel the qmodel
     */
    public InteractiveCmd(ParseModel qmodel) {
        this.type = Cmd.Select;
        this.qmodel = qmodel;
    }

    /**
     * Instantiates a new interactive cmd for a string based command.
     *
     * @param cmd   the cmd
     * @param ident the ident
     */
    public InteractiveCmd(Cmd cmd, String ident) {
        this.type = cmd;
        this.ident = ident;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public Cmd getType() {
        return type;
    }

    /**
     * Gets the parse model for a select.
     *
     * @return the q model
     */
    public ParseModel getParseModel() {
        return qmodel;
    }

    /**
     * Gets the ident.
     *
     * @return the ident
     */
    public String getIdent() {
        return ident;
    }

}
