import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class BufferedReaderTest {

    private static final byte CARRIAGE_RETURN = '\r';

    private static final byte LINE_FEED = '\n';


    @Test
    @DisplayName("readLine 메서드 테스트- CRLF 를 포함하지 않은 문자열까지를 read 한다")
    public void test0() throws IOException {
        final String nonEndedString = "abc dfe\nsdfadfsdf";

        final BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(nonEndedString.getBytes())));

        final String readString = reader.readLine();

        assertThat(readString).isEqualTo("abc dfe");
    }

    @Test
    @DisplayName("read 메서드 테스트 - read 로 읽은 값을 char 타입으로 변환한 이후에도 line terminator 로 읽을 수 있다")
    public void test1() {
        byte[] strBytes = {'a', 'b', 'c', '\r', '\n'};
        final int maxCount = 5;
        int cur = 0;
        try (
                InputStream inputStream = new ByteArrayInputStream(strBytes);
        ) {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            while (true) {
                final char myChar = (char) reader.read();
//                final int myChar = reader.read();

                if (myChar == '\r') {
                    System.out.println(String.format("Index %d is carriage return marker", cur));
                    assertThat(cur).isEqualTo(3);
                }
                if (myChar == '\n') {
                    System.out.println(String.format("Index %d is line feed marker", cur));
                    assertThat(cur).isEqualTo(4);
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
    @DisplayName("\\r 과 \\n 은 서로 다른 char 로 인식한다")
    public void test3() {
        char cr = '\r';
        char lf = '\n';
        assertThat(cr).isNotEqualTo(lf);
    }

    @Test
    @DisplayName("-1 을 char 로 변환 후 int 로 다시 변환할 경우 stackoverflow 로 인해 2^16 -1 인 65535 이다")
    public void test4() {
        char targetChar = (char) -1;

        assertThat((int) targetChar).isEqualTo(65535);
    }

    @Nested
    class Context_HangulInput {

        final char[] testChars = new char[]{'h', 'e', 'l', 'l'};
        final byte[] hangul = new String(new char[]{'안', '녕'}).getBytes(StandardCharsets.UTF_8);
        final byte[] english8 = new String(testChars).getBytes(StandardCharsets.UTF_8);
        final byte[] english16 = new String(testChars).getBytes(StandardCharsets.UTF_16);

        // 실패시 -Dfile.encoding 을 확인해보자
        @DisplayName("InputStreamReader 의 의 default encoding(CharSet) 은 한글 인코딩이 가능하다")
        @Test
        public void then_encode_hangul() throws IOException {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(hangul)));
            final String readLine = reader.readLine();
            assertThat(readLine).isEqualTo("안녕");
        }

        @DisplayName("Default Charset 을 사용 중인 Reader 의 read() 는 한글 한 글자를 읽을 수 있다")
        @Test
        public void then_read_one_character() throws IOException {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(hangul)));
            final String readCharacter = String.valueOf((char) reader.read());

            assertThat(readCharacter).isEqualTo("안");
            assertThat(readCharacter.getBytes()).hasSizeGreaterThan(1);
        }

        @DisplayName("Default CharSet 은 UTF-8 이다")
        @Test
        public void then_system_property_is_UTF_8() {
            assertThat(Charset.defaultCharset()).isEqualTo(StandardCharsets.UTF_8);
        }

        @DisplayName("UTF-8 인코딩 으로 읽어온 한글 character 는 알파벳 character 보다 많은 공간을 차지한다")
        @Test
        public void then_one_character_is_bigger_than_one_byte() throws IOException {
            final BufferedReader hangulReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(hangul), StandardCharsets.UTF_8));
            final BufferedReader englishReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(english8), StandardCharsets.UTF_8));
            final String readHangulCharacter = String.valueOf((char) hangulReader.read());
            final String readEnglishCharacter = String.valueOf((char) englishReader.read());

            assertThat(readHangulCharacter.getBytes(StandardCharsets.UTF_8)) // 시스템 파일 인코딩이 UTF-8 인 경우 getBytes() 라고만 해도 된다
                    .hasSizeGreaterThan(readEnglishCharacter.getBytes(StandardCharsets.UTF_8).length);
        }

        @DisplayName("UTF-16 인코딩 으로 읽어온 알파벳 character 는 UTF-8 인코딩으로 읽어온 알파벳 character 는 동일한 code point 값을 갖는다")
        @Test
        public void then_UTF_16_min_is_bigger_than_UTF_8() throws IOException {

            final BufferedReader utf8Reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(english8), StandardCharsets.UTF_8));
            final BufferedReader utf16Reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(english16), StandardCharsets.UTF_16));

            final int intRead16 = utf16Reader.read();
            final int intRead8 = utf8Reader.read();

            Assertions.assertThat(intRead16).isEqualTo(intRead8);
        }

        @DisplayName("UTF-16 으로 인코딩된, 알파벳만으로 이루어진 문자열은 2 + 2*(length) 만큼의 byte 를 차지한다")
        @Test
        public void then_utf16() throws IOException {
            // given
            final BufferedReader utf16Reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(english16), StandardCharsets.UTF_16));
            final String testStr = utf16Reader.readLine();

            final int expectedSize = 2 + 2 * testChars.length;

            // when then
            assertThat(testStr.getBytes(StandardCharsets.UTF_16)).hasSize(expectedSize);
        }
    }
}
