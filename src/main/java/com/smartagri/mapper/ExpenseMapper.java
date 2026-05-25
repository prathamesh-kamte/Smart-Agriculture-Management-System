package com.smartagri.mapper;

import com.smartagri.domain.dto.ExpenseDto;
import com.smartagri.domain.entity.Expense;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {

    @Mapping(source = "crop.id", target = "cropId")
    @Mapping(source = "crop.cropName", target = "cropName")
    ExpenseDto toDto(Expense expense);

    @Mapping(target = "crop", ignore = true)
    Expense toEntity(ExpenseDto expenseDto);
}
