package com.db.awmd.challenge.event;

import org.springframework.context.ApplicationEvent;

public class GenericEvent<Payload> extends ApplicationEvent {

  public GenericEvent(Payload payload) {
    super(payload);
  }

  public Payload payload() {
    //noinspection unchecked
    return (Payload) getSource();
  }
}
