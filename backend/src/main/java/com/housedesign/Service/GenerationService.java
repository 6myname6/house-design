package com.housedesign.Service;

import com.housedesign.dto.response.GenerationResponse;

public interface GenerationService {

    GenerationResponse generateRequest(Long projectId);

}
