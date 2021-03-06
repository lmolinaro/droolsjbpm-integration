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

package org.kie.server.api.marshalling;

public class MarshallingException
        extends RuntimeException {

    /** Generated serial version UID */
    private static final long serialVersionUID = -4588806446951069542L;

    public MarshallingException(String msg, Throwable cause) {
        super(msg, cause);
    }
    
    public MarshallingException(String msg) {
        super(msg);
    }
}
