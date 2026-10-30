package architecture.fixture.adapter;

import architecture.fixture.application.DescribeBoundary;
import architecture.fixture.domain.BoundaryDescriptor;

public final class BoundaryAdapter {

    private final DescribeBoundary useCase;

    public BoundaryAdapter(DescribeBoundary useCase) {
        this.useCase = useCase;
    }

    public String describe(String name) {
        return useCase.execute(new BoundaryDescriptor(name));
    }
}
