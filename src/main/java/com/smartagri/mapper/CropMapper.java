package com.smartagri.mapper;

import com.smartagri.domain.dto.CropDto;
import com.smartagri.entity.Crop;
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

    com.smartagri.domain.enums.Season mapSeason(com.smartagri.entity.Season season);
    com.smartagri.entity.Season mapSeason(com.smartagri.domain.enums.Season season);

    com.smartagri.domain.enums.CropStatus mapCropStatus(com.smartagri.entity.CropStatus status);
    com.smartagri.entity.CropStatus mapCropStatus(com.smartagri.domain.enums.CropStatus status);
}
