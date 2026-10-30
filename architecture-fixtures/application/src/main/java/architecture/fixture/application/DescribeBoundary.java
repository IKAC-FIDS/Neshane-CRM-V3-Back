package architecture.fixture.application;

import architecture.fixture.domain.BoundaryDescriptor;

public final class DescribeBoundary {

    public String execute(BoundaryDescriptor descriptor) {
        return "boundary:" + descriptor.name();
    }
}
