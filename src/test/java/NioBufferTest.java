import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class NioBufferTest {

    // read 를 위해 position 을 옮긴다고 이해
    @DisplayName("flip 메서드를 호출해 read 모드로 전환시, limit 이 기존의 position 값으로 변경되고 position 은 0 으로 변경된다")
    @Test
    void flip_change_limit() {
        // given
        final int fixedSize = 5;
        final ByteBuffer buffer = ByteBuffer.allocate(fixedSize);

        buffer.put((byte) 1);
        buffer.put((byte) 2);
        buffer.put((byte) 3);

        assertThat(buffer.limit()).isEqualTo(fixedSize);
        final int prePosition = buffer.position();

        // when
        buffer.flip();

        // then
        assertThat(buffer.limit()).isEqualTo(prePosition);
        assertThat(buffer.position()).isEqualTo(0);
    }

    @Nested
    @DisplayName("clear 메서드는 ")
    class Clear {

        @Test
        @DisplayName("position 을 0으로 옮긴다")
        void clear_test() {
            ByteBuffer buffer = ByteBuffer.allocate(6);

            buffer.put((byte) 1);
            buffer.put((byte) 2);
            buffer.put((byte) 3);

            buffer.flip();

            buffer.clear();

            assertThat(buffer.position()).isEqualTo(0);
        }

        // buffer 내의 값을 삭제하지 않고 clear 한 효과를 주기 위한것으로 이해
        @Test
        @DisplayName("limit 값을 0 으로 변경한다")
        void change_limit() {
            ByteBuffer buffer = ByteBuffer.allocate(6);

            buffer.put((byte) 1);
            buffer.put((byte) 2);
            buffer.put((byte) 3);

            buffer.clear();

            buffer.flip();

            assertThat(buffer.limit()).isEqualTo(0);
        }

        @Test
        @DisplayName("get 호출시 0 이 된 limit 으로 인해 BufferUnderFlowException 이 발생한다")
        void get_throw_UnderflowException() {
            ByteBuffer buffer = ByteBuffer.allocate(6);

            buffer.put((byte) 1);
            buffer.put((byte) 2);
            buffer.put((byte) 3);

            buffer.clear();

            buffer.flip();

            assertThatExceptionOfType(BufferUnderflowException.class)
                .isThrownBy(buffer::get);
        }

        @Test
        @DisplayName("buffer 내의 기존의 값을 삭제하지 않는다")
        void not_remove() {
            // given
            final int fixedSize = 6;
            ByteBuffer buffer = ByteBuffer.allocate(fixedSize);

            buffer.put((byte) 1);
            buffer.put((byte) 2);
            buffer.put((byte) 3);

            // when
            buffer.clear();

            buffer.flip();
            buffer.limit(fixedSize);

            byte[] copyStore = new byte[fixedSize];
            buffer.get(copyStore);

            // then
            assertThat(copyStore).contains(new byte[]{1, 2, 3, 0, 0, 0});
        }

    }
}
