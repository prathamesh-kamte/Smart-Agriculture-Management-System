package com.smartagri.mapper;

import com.smartagri.domain.dto.CropDto;
import com.smartagri.domain.entity.Crop;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CropMapper {

    @Mapping(source = "farmer.id", target = "farmerId")
    @Mapping(source = "farmer.fullName", target = "farmerName")
    CropDto toDto(Crop crop);

    @Mapping(target = "farmer", ignore = true)
    @Mapping(target = "expenses", ignore = true)
    Crop toEntity(CropDto cropDto);
}
