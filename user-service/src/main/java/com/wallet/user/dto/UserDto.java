package com.wallet.user.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class UserDto implements Serializable {

    private static final long serialVersionUID = 1l;

    @NotNull
    private String name;

    @NotNull
    private String email;

    @NotNull
    private String phone;

    @NotNull
    private String kycNumber;
    // 1234-1345-12345 = 12**-****-**45

}
