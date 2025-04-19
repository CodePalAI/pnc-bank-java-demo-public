package io.pnc.bank.demo.mapper;

import io.pnc.bank.demo.dto.TransactionDTO;
import io.pnc.bank.demo.model.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "accountNumber", source = "account.accountNumber")
    TransactionDTO toDto(Transaction transaction);

    List<TransactionDTO> toDtoList(List<Transaction> transactions);
} 