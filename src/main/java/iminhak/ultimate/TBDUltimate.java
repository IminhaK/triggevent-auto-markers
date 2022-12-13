package iminhak.ultimate;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
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

@CalloutRepo(name = "TBD Ultimate Automarkers", duty = KnownDuty.TBD_Ultimate)
public class TBDUltimate extends AutoChildEventHandler implements FilteredEventHandler {

    private static final Logger log = LoggerFactory.getLogger(TBDUltimate.class);

    private final BooleanSetting useAutomarks;

    private final BooleanSetting useSomeMechanic;

    public TBDUltimate(XivState state, StatusEffectRepository buffs, PersistenceProvider pers) {
        this.state = state;
        this.buffs = buffs;
        this.useAutomarks = new BooleanSetting(pers, "triggers.tbd.use-auto-markers", false);

        this.useSomeMechanic = new BooleanSetting(pers, "triggers.tbd.use-something", false);
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
        context.accept(new ClearAutoMarkRequest());
    }

    @AutoFeed
    private final SequentialTrigger<BaseEvent> someMechanicThatIsOnlyCastOnce = SqtTemplates.sq(60_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(0x0),
            (e1, s) -> {
                if(getUseAutomarks().get() && getUseSomeMechanic().get()) {

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

    public BooleanSetting getUseSomeMechanic() {
        return useSomeMechanic;
    }
}
