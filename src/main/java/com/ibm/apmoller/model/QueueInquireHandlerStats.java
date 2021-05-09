package com.ibm.apmoller.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class QueueInquireHandlerStats {

    private int queueState;
    private String applicationTag;
    private String userId;
    private int sequence;

}
