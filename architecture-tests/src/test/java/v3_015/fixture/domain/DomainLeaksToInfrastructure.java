package v3_015.fixture.domain;

import v3_015.fixture.infrastructure.FakeInfrastructure;

public final class DomainLeaksToInfrastructure {

    private final FakeInfrastructure infrastructure;

    public DomainLeaksToInfrastructure(FakeInfrastructure infrastructure) {
        this.infrastructure = infrastructure;
    }

    public String execute() {
        return infrastructure.load();
    }
}
