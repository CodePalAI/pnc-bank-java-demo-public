package io.pnc.bank.demo.mapper;

import io.pnc.bank.demo.dto.AccountDTO;
import io.pnc.bank.demo.dto.CreateAccountRequest;
import io.pnc.bank.demo.dto.UpdateAccountRequest;
import io.pnc.bank.demo.model.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AccountMapper {

    AccountDTO toDto(Account account);

    List<AccountDTO> toDtoList(List<Account> accounts);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    @Mapping(target = "balance", source = "initialDeposit", defaultExpression = "java(java.math.BigDecimal.ZERO)")
    @Mapping(target = "status", constant = "ACTIVE")
    Account toEntity(CreateAccountRequest createAccountRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    void updateAccountFromDto(UpdateAccountRequest updateAccountRequest, @MappingTarget Account account);
} 