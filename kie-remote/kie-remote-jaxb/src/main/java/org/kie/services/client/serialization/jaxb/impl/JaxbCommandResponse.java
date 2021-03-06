/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.services.client.serialization.jaxb.impl;


public interface JaxbCommandResponse<T> {

    /**
     * @return The index of the command in the {@link JaxbCommandsRequest#getCommands()} list. 
     */
    public Integer getIndex();

    /**
     * This method is necessary for the YAML framework (which expects getters *and* setters) to work with these objects. 
     * @param index
     */
    public void setIndex(Integer index);
    
    /**
     * @return The (simple) name of the command class that generated this response. 
     */
    public String getCommandName();
    
     /**
      * This method is necessary for the YAML framework (which expects getters *and* setters) to work with these objects. 
      * @param cmdName
      */
    public void setCommandName(String cmdName);

    /**
     * @return The result object generated by the command. If the command does not return an object, null.
     */
    public T getResult();

      /**
       * This method is necessary for the YAML framework (which expects getters *and* setters) to work with these objects. 
       * @param result
       */
    public void setResult(T result);

}