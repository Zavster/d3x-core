/*
 * Copyright (C) 2018 D3X Systems - All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.d3x.core.mail;

import java.util.Collections;
import java.util.List;


@lombok.Builder
@lombok.AllArgsConstructor
public class MailMessage {

    /** The subject text for mail message */
    @lombok.NonNull @lombok.Getter private String subject;
    /** The list of recipient email addresses */
    @lombok.NonNull private List<String> recipients;
    /** The message body for mail message */
    @lombok.NonNull @lombok.Getter private String body;
    /** The content type for message body */
    @lombok.NonNull @lombok.Getter private String contentType;

    /**
     * Returns the list of recipient email addresses
     * @return  the list of recipient addresses
     */
    public List<String> getRecipients() {
        return Collections.unmodifiableList(recipients);
    }
}
