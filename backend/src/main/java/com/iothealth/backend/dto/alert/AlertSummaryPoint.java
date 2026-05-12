package com.iothealth.backend.dto.alert;

import java.time.Instant;

public record AlertSummaryPoint(Instant hour, long critical, long warning) {}
