package com.github.yktakaha4.watsonmusic.api.endpoint;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.yktakaha4.watsonmusic.api.ClientActionType;
import com.github.yktakaha4.watsonmusic.api.entity.PostedText;
import com.github.yktakaha4.watsonmusic.api.entity.Request;
import com.github.yktakaha4.watsonmusic.api.entity.Succeed;
import com.github.yktakaha4.watsonmusic.api.util.Socket;
import com.github.yktakaha4.watsonmusic.service.RequestService;

@RestController
@RequestMapping("/api/requests")
public class Requests {
  private final RequestService requestService;
  private final Socket socket;

  @Autowired
  public Requests(RequestService requestService, Socket socket) {
    this.requestService = requestService;
    this.socket = socket;
  }

  @RequestMapping(method = RequestMethod.GET)
  public List<Request> getRequests(@RequestParam(name = "userId", required = true) String userId) {
    List<Request> requests = requestService.getUnplayedRequests().stream().map((source) -> {
      return createRequest(source, userId);
    }).collect(Collectors.toList());

    return requests;
  }

  @RequestMapping(method = RequestMethod.POST)
  public Succeed postRequest(@RequestBody @Validated PostedText postedText,
      @RequestParam(name = "userId", required = true) String userId) {
    requestService.appendRequest(postedText.getText(), userId);

    socket.sendToClients(Arrays.asList(ClientActionType.GET_REQUESTS));
    return new Succeed();
  }

  @RequestMapping(path = "/{requestId}", method = RequestMethod.DELETE)
  public Succeed deleteRequest(@PathVariable("requestId") String requestId,
      @RequestParam(name = "userId", required = true) String userId) {
    requestService.removeRequestByTag(requestId, userId);

    socket.sendToClients(Arrays.asList(ClientActionType.GET_REQUESTS));
    return new Succeed();
  }

  private Request createRequest(com.github.yktakaha4.watsonmusic.model.Request source, String userId) {
    boolean yours = StringUtils.equalsIgnoreCase(userId, source.getUserTag());
    boolean playing = source.getPlayingAt() != null;
    Request request = new Request();
    request.setRequestId(source.getRequestTag());
    request.setText(source.getText());
    switch(source.getUserTag()) {
    case "GOOGLE_ASSISTANT":
      request.setType(source.getUserTag());
      break;
    default:
      request.setType("WEB");
      break;
    }
    request.setPostedAt(ZonedDateTime.ofLocal(source.getCreatedAt(), ZoneId.of("UTC"), null).toEpochSecond());
    request.setPlaying(playing);
    request.setDeletable(yours && !playing);
    request.setYours(yours);
    return request;
  }

}
