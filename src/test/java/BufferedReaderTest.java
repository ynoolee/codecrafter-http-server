import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class BufferedReaderTest {

    private static final byte CARRIAGE_RETURN = '\r';

    private static final byte LINE_FEED = '\n';


    @Test
    @DisplayName("readLine 메서드 테스트- CRLF 를 포함하지 않은 문자열까지를 read 한다")
    public void test0() throws IOException {
        final String nonEndedString = "abc dfe\nsdfadfsdf";

        final BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(nonEndedString.getBytes())));

        final String readString = reader.readLine();

        Assertions.assertThat(readString).isEqualTo("abc dfe");
    }

    @Test
    @DisplayName("read 메서드 테스트 - read 로 읽은 값을 char 타입으로 변환한 이후에도 line terminator 로 읽을 수 있다")
    public void test1() {
        byte[] strBytes = {'a', 'b', 'c', '\r', '\n'};
//        final String string = "abc\r";
        final int maxCount = 5;
        int cur = 0;
        try (
                InputStream inputStream = new ByteArrayInputStream(strBytes);
        ) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            while (true) {
                final char myChar = (char) reader.read();
//                final int myChar = reader.read();

                if(myChar == '\r') {
                    System.out.println(String.format("Index %d is carriage return marker", cur));
                    Assertions.assertThat(cur).isEqualTo(3);
                }
                if(myChar == '\n') {
                    System.out.println(String.format("Index %d is line feed marker", cur));
                    Assertions.assertThat(cur).isEqualTo(4);
                }

                System.out.println(String.format("MyChar : %c", myChar));
//                System.out.println(String.format("MyChar value: %d", myChar));

                cur++;
                if (cur == maxCount) {
                    break;
                }
            }

        } catch (IOException ex) {

        }
    }

    @Test
    @DisplayName("\r 과 \n 은 서로 다른 char 로 인식한다")
    public void test3() {
        char cr = '\r';
        char lf = '\n';
        Assertions.assertThat(cr).isEqualTo(lf);
    }

    @Test
    @DisplayName("-1 을 char 로 변환 후 int 로 다시 변환할 경우 stackoverflow 로 인해 2^16 -1 인 65535 이다")
    public void test4() {
        char targetChar = (char)-1;

        Assertions.assertThat((int)targetChar).isEqualTo(65535);
    }

    @Test
    @DisplayName("미리 할당해둔 char array 에 0 으로 채울 경우")
    public void test5() {
        // 예제용 char 배열
        char[] charArray = {'a', 'b', 'c', 'd', 'e', 'f', 'g'};
        int curIdx = 3; // 이 인덱스 이후의 값을 지우고자 함

        // 배열 상태 출력 (지우기 전)
        System.out.println("Before clearing: " + Arrays.toString(charArray));

        // curIdx 이후의 모든 값을 \0으로 설정
        Arrays.fill(charArray, curIdx, charArray.length, '\0');

        // 배열 상태 출력 (지운 후)
        System.out.println("After clearing: " + String.valueOf(charArray));
    }
}
