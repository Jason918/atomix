/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kuujo.copycat.protocol.raft.rpc;

import net.kuujo.copycat.io.Buffer;
import net.kuujo.copycat.io.util.ReferenceManager;
import net.kuujo.copycat.protocol.raft.RaftError;

import java.util.Objects;

/**
 * Protocol write response.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public class WriteResponse extends AbstractResponse<WriteResponse> {
  private static final ThreadLocal<Builder> builder = new ThreadLocal<Builder>() {
    @Override
    protected Builder initialValue() {
      return new Builder();
    }
  };

  /**
   * Returns a new write response builder.
   *
   * @return A new write response builder.
   */
  public static Builder builder() {
    return builder.get().reset();
  }

  /**
   * Returns a write response builder for an existing response.
   *
   * @param response The response to build.
   * @return The write response builder.
   */
  public static Builder builder(WriteResponse response) {
    return builder.get().reset(response);
  }

  private Buffer result;

  public WriteResponse(ReferenceManager<WriteResponse> referenceManager) {
    super(referenceManager);
  }

  @Override
  public Type type() {
    return Type.WRITE;
  }

  /**
   * Returns the write result.
   *
   * @return The write result.
   */
  public Buffer result() {
    return result;
  }

  @Override
  public void readObject(Buffer buffer) {
    status = Response.Status.forId(buffer.readByte());
    if (status == Response.Status.OK) {
      error = null;
      result = buffer.slice();
    } else {
      error = RaftError.forId(buffer.readByte());
    }
  }

  @Override
  public void writeObject(Buffer buffer) {
    buffer.writeByte(status.id());
    if (status == Response.Status.OK) {
      buffer.write(result);
    } else {
      buffer.writeByte(error.id());
    }
  }

  @Override
  public void close() {
    if (result != null)
      result.release();
    super.close();
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, result);
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof WriteResponse) {
      WriteResponse response = (WriteResponse) object;
      return response.status == status
        && ((response.result == null && result == null)
        || response.result != null && result != null && response.result.equals(result));
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format("%s[status=%s, result=%s]", getClass().getSimpleName(), status, result);
  }

  /**
   * Write response builder.
   */
  public static class Builder extends AbstractResponse.Builder<Builder, WriteResponse> {

    private Builder() {
      super(WriteResponse::new);
    }

    /**
     * Sets the write response result.
     *
     * @param result The response result.
     * @return The response builder.
     */
    public Builder withResult(Buffer result) {
      response.result = result;
      return this;
    }

    @Override
    public WriteResponse build() {
      super.build();
      if (response.result != null)
        response.result.acquire();
      return response;
    }

    @Override
    public int hashCode() {
      return Objects.hash(response);
    }

    @Override
    public boolean equals(Object object) {
      return object instanceof Builder && ((Builder) object).response.equals(response);
    }

    @Override
    public String toString() {
      return String.format("%s[response=%s]", getClass().getCanonicalName(), response);
    }

  }

}
