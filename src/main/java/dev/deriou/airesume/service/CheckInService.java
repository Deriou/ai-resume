package dev.deriou.airesume.service;

import dev.deriou.airesume.vo.CheckInStatusVO;

public interface CheckInService {

    CheckInStatusVO today();

    CheckInStatusVO checkIn();
}
