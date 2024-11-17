package com.dws.challenge.domain;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class TransferRequest {
	
	private String accountFrom;
    private String accountTo;
    private BigDecimal amount;

}
