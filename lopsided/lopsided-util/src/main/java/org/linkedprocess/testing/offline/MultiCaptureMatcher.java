package org.linkedprocess.testing.offline;

import static org.easymock.EasyMock.reportMatcher;
import org.easymock.IArgumentMatcher;

import java.util.Collection;

public class MultiCaptureMatcher<T> implements IArgumentMatcher {

    Collection<T> captureDestination;

    public MultiCaptureMatcher(Collection<T> captureDestination) {
        this.captureDestination = captureDestination;
    }

    public void appendTo(StringBuffer buffer) {
        buffer.append("multiCapture(").append(captureDestination.toString()).append(")");
    }

    @SuppressWarnings("unchecked")
	public boolean matches(Object actual) {
        captureDestination.add((T) actual);
        return true;
    }

    public static <S> S multiCapture(Collection<S> destination) {
        reportMatcher(new MultiCaptureMatcher<S>(destination));
        return null;
    }
}
