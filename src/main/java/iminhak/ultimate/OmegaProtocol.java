package iminhak.ultimate;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyRecommenceEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.marks.ClearAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@CalloutRepo(name = "Iminha's Omega Protocol", duty = KnownDuty.None)
public class OmegaProtocol extends AutoChildEventHandler implements FilteredEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OmegaProtocol.class);

    private boolean autoMarking = false;

    private final BooleanSetting useAutomarks;

    private final BooleanSetting useCircleProgram;

    public OmegaProtocol(XivState state, StatusEffectRepository buffs, PersistenceProvider pers) {
        this.state = state;
        this.buffs = buffs;
        this.useAutomarks = new BooleanSetting(pers, "triggers.top.use-auto-markers", false);
        this.useCircleProgram = new BooleanSetting(pers, "triggers.top.use-something", false);
    }

    private final XivState state;
    private final StatusEffectRepository buffs;

    private XivState getState() {
        return this.state;
    }

    private StatusEffectRepository getBuffs() {
        return this.buffs;
    }

    @Override
    public boolean enabled(EventContext context) {
        return state.dutyIs(KnownDuty.None);
    }

    @HandleEvents
    public void reset(EventContext context, DutyRecommenceEvent drce) {
        if(autoMarking) {
            context.accept(new ClearAutoMarkRequest());
            autoMarking = false;
        }
    }

    private final Predicate<BuffApplied> circleProgramNumber = ba -> {
        long id = ba.getBuff().getId();
        return id >= 0x0 && id <= 0x0; //TODO: Circle Program dive numbers, low then high ID
    };

    @AutoFeed
    private final SequentialTrigger<BaseEvent> circleProgram = SqtTemplates.sq(60_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(0x0), //TODO: Insert correct ID for Circle Program cast
            (e1, s) -> {
                if(getUseCircleProgram().get() && getUseAutomarks().get()) {
                    autoMarking = true;
                    log.info("Circle Program: start");
                    List<BuffApplied> lineDebuffs = s.waitEvents(8, BuffApplied.class, circleProgramNumber); //TODO: Confirm number of line debuffs applied
                    Optional<BuffApplied> lineDebuffOnPlayer = lineDebuffs.stream().filter(ba -> ba.getTarget().isThePlayer()).findFirst();
                    if (lineDebuffOnPlayer.isPresent()) {
                        int linePos = (int) lineDebuffOnPlayer.get().getBuff().getId() - 0x0 + 1; //TODO: Insert line buff offset (first buff minus one)
                        boolean inTower = getBuffs().getBuffs().stream().anyMatch(ba -> ba.getBuff().getId() == 0x0 && ba.getTarget().isThePlayer()); //TODO: Get Patch ID

                        s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x0)); //TODO: Mustard bomb cast ID

                        //TODO: If only 4 line debuffs, create prio for who takes first/second mustards
                    }
                }
            });

    @AutoFeed
    private final SequentialTrigger<BaseEvent> someMechanicThatIsCaseMultipleTimes = SqtTemplates.multiInvocation(60_000, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x0),
            this::firstUse,
            this::secondUse);

    private void firstUse(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {

    }

    private void secondUse(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {

    }

    public BooleanSetting getUseAutomarks() {
        return useAutomarks;
    }

    public BooleanSetting getUseCircleProgram() {
        return useCircleProgram;
    }
}
