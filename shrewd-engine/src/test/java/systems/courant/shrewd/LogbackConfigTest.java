package systems.courant.shrewd;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Logback configuration (#187)")
class LogbackConfigTest {

    @Nested
    @DisplayName("logback.xml is loaded by default")
    class ConfigLoaded {

        @Test
        @DisplayName("should use LoggerContext (not a fallback/NOP implementation)")
        void shouldUseLogbackContext() {
            assertThat(LoggerFactory.getILoggerFactory())
                    .isInstanceOf(LoggerContext.class);
        }

        @Test
        @DisplayName("logback-test.xml should set root logger to DEBUG in test scope")
        void shouldSetRootToDebugInTestScope() {
            Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            assertThat(root.getLevel()).isEqualTo(Level.DEBUG);
        }
    }

    @Nested
    @DisplayName("boilerplate logger removed")
    class NoBoilerplate {

        @Test
        @DisplayName("should not have an explicit logger for com.lordofthejars.foo")
        void shouldNotHaveBoilerplateLogger() {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            // getLoggerList() returns only loggers that have been explicitly configured or used;
            // a logger that was never configured will not appear with an explicit level.
            Logger boilerplate = context.exists("com.lordofthejars.foo");
            // exists() returns null if the logger was never created in the context
            assertThat(boilerplate).isNull();
        }
    }

    @Nested
    @DisplayName("production config file exists and is well-formed")
    class ProductionConfig {

        @Test
        @DisplayName("logback.xml should be on the classpath")
        void shouldFindLogbackXml() {
            assertThat(getClass().getResource("/logback.xml")).isNotNull();
        }

        @Test
        @DisplayName("logger.xml (old name) should not be on the classpath")
        void shouldNotFindOldLoggerXml() {
            assertThat(getClass().getResource("/logger.xml")).isNull();
        }

        @Test
        @DisplayName("logback-test.xml should be on the classpath in test scope")
        void shouldFindLogbackTestXml() {
            assertThat(getClass().getResource("/logback-test.xml")).isNotNull();
        }
    }
}
