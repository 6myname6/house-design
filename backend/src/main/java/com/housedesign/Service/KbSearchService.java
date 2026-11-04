package com.housedesign.Service;

import com.housedesign.dto.request.KbSearchRequest;
import com.housedesign.dto.response.KbSearchResponse;

/**
 * KbSearchService
 */
public interface KbSearchService {

    KbSearchResponse search(KbSearchRequest request);

}
