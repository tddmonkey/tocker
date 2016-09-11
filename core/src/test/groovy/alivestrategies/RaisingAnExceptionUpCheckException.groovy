package alivestrategies

import com.shazam.tocker.AliveStrategies
import com.shazam.tocker.AliveStrategy
import com.shazam.tocker.UpChecks
import spock.lang.Specification


class RaisingAnExceptionUpCheckException extends Specification {
    def "is not alive when exception is raised"() {
        given:
            def strategy = UpChecks.exceptionIsDown({ throw new TestException() })

        expect:
            strategy.get() == false
    }

    def "is alive when no exception is raised"() {
        given:
            def strategy = UpChecks.exceptionIsDown({ })

        expect:
            strategy.get() == true
    }
}

class TestException extends Exception { }