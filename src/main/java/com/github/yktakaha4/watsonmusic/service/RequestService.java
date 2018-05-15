package com.github.yktakaha4.watsonmusic.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.yktakaha4.watsonmusic.ApplicationException;
import com.github.yktakaha4.watsonmusic.mapper.RequestMapper;
import com.github.yktakaha4.watsonmusic.model.Request;
import com.github.yktakaha4.watsonmusic.util.IdentifierUtils;

@Service
public class RequestService {
  private final RequestMapper requestMapper;
  private final IdentifierUtils identifierUtils;

  @Autowired
  public RequestService(RequestMapper requestMapper, IdentifierUtils identifierUtils) {
    this.requestMapper = requestMapper;
    this.identifierUtils = identifierUtils;
  }

  @Transactional(readOnly = true)
  public List<Request> getUnplayedRequests() {
    return requestMapper.selectByPlayedAtIsNull();
  }

  @Transactional(readOnly = true)
  public Optional<Request> getPlayableRequest() {
    return getUnplayedRequests().stream().findFirst();
  }

  @Transactional(readOnly = true)
  public Optional<Request> getRequestByTag(String requestTag) {
    return Optional.ofNullable(requestMapper.selectByTag(requestTag));
  }

  @Transactional
  public void appendRequest(String text, String userId) {
    Request request = new Request();
    request.setRequestTag(identifierUtils.newTag());
    request.setText(text);
    request.setUserTag(userId);
    request.setCreatedAt(LocalDateTime.now());
    request.setPlayedAt(null);
    requestMapper.insert(request);
  }

  @Transactional
  public void appendRequestByVoice(String text, String type, LocalDateTime localDateTime) {
    Request request = new Request();
    request.setRequestTag(identifierUtils.newTag());
    request.setText(text);
    request.setUserTag(type);
    request.setCreatedAt(localDateTime);
    request.setPlayedAt(null);
    requestMapper.insert(request);
  }

  @Transactional
  public void markRequestIsPlaying(String requestTag) {
    requestMapper.updatePlayingAtByTag(requestTag, LocalDateTime.now());
  }

  @Transactional
  public void markRequestIsPlayed(String requestTag) {
    requestMapper.updatePlayedAtByTag(requestTag, LocalDateTime.now());
  }

  @Transactional
  public void removeRequestByTag(String requestTag, String userTag) {
    Request request = getRequestByTag(requestTag)
        .orElseThrow(() -> new ApplicationException(HttpStatus.BAD_REQUEST));

    if (!StringUtils.equalsIgnoreCase(request.getUserTag(), userTag)) {
      throw new ApplicationException(HttpStatus.BAD_REQUEST);
    }

    requestMapper.deleteByTag(requestTag);
  }

}
