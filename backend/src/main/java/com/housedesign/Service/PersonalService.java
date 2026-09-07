package com.housedesign.Service;

import com.housedesign.dto.request.UpdateRequest;
import com.housedesign.dto.response.PersonalDataResponse;

public interface PersonalService {

    PersonalDataResponse searchById();

    PersonalDataResponse updatePersonalData(UpdateRequest updateRequest);

}
