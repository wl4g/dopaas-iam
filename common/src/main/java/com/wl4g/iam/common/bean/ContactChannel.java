/*
 * Copyright 2017 ~ 2025 the original authors James Wong.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wl4g.iam.common.bean;

import com.wl4g.infra.core.bean.BaseBean;

public class ContactChannel extends BaseBean {

    private static final long serialVersionUID = -7546448616357790576L;

    private Long contactId;

    private String kind;

    private String primaryAddress;

    private Integer timeOfFreq;

    private Integer numOfFreq;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public String getPrimaryAddress() {
        return primaryAddress;
    }

    public void setPrimaryAddress(String primaryAddress) {
        this.primaryAddress = primaryAddress == null ? null : primaryAddress.trim();
    }

    public Integer getTimeOfFreq() {
        return timeOfFreq;
    }

    public void setTimeOfFreq(Integer timeOfFreq) {
        this.timeOfFreq = timeOfFreq;
    }

    public Integer getNumOfFreq() {
        return numOfFreq;
    }

    public void setNumOfFreq(Integer numOfFreq) {
        this.numOfFreq = numOfFreq;
    }

}