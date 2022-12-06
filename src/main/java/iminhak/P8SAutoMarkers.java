package iminhak;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.xivdata.data.duties.KnownDuty;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.events.triggers.marks.adv.SpecificAutoMarkRequest;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@CalloutRepo(name = "P8S", duty = KnownDuty.P8S)
public class P8SAutoMarkers extends AutoChildEventHandler {

    private static final Logger log = LoggerFactory.getLogger(P8SAutoMarkers.class);

    private final BooleanSetting useAutoMarks;

    private final BooleanSetting useLimitlessDesolation;
    private final BooleanSetting LDSupportAttack;

    private final BooleanSetting useHC1;

    private final BooleanSetting useHC2;

    private final BooleanSetting useLD;

    public P8SAutoMarkers(XivState state, StatusEffectRepository buffs, PersistenceProvider pers) {
        this.useAutoMarks = new BooleanSetting(pers, "triggers.p8s.use-auto-markers", false);

        this.useLimitlessDesolation = new BooleanSetting(pers, "triggers.p8s.use-dominion", false);
        this.LDSupportAttack = new BooleanSetting(pers, "triggers.p8s.use-LD-sup-attack", false);

        this.useHC1 = new BooleanSetting(pers, "triggers.p8s.use-hc1", false);

        this.useHC2 = new BooleanSetting(pers, "triggers.p8s.use-hc2", false);

        this.useLD = new BooleanSetting(pers, "triggers.p8s.use-ld", false);
    }

    @AutoFeed
    private final SequentialTrigger<BaseEvent> limitlessDesolation = SqtTemplates.sq(60_000, AbilityCastStart.class,
            acs -> acs.abilityIdMatches(0x75ED),
            (e1, s) -> {
                if(getUseAutoMarks().get() && getUseLimitlessDesolation().get()) {
                    log.info("Limitless Desolation markers are enabled");
                    for (int i = 1; i <= 4; i++) {
                        boolean inverse = getLDSupportAttack().get();
                        List<AbilityUsedEvent> events = s.waitEventsQuickSuccession(2, AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x75F0) && aue.isFirstTarget(), Duration.ofMillis(400));
                        //Expect 1 sup and 1 dps
                        Optional<AbilityUsedEvent> dps = events.stream().filter(aue -> {
                            XivCombatant target = aue.getTarget();
                            return (target instanceof XivPlayerCharacter pc) && pc.getJob().isDps();
                        }).findAny();
                        Optional<AbilityUsedEvent> sup = events.stream().filter(aue -> {
                            XivCombatant target = aue.getTarget();
                            return (target instanceof XivPlayerCharacter pc) && !pc.getJob().isDps();
                        }).findAny();

                        if(dps.isPresent()) {
                            XivPlayerCharacter dpsPlayer = (XivPlayerCharacter) dps.get().getTarget();
                            XivPlayerCharacter supPlayer = (XivPlayerCharacter) sup.get().getTarget();
                            s.accept(new SpecificAutoMarkRequest(dpsPlayer, switch (i) {
                                case 1 -> !inverse ? MarkerSign.ATTACK1 : MarkerSign.BIND1;
                                case 2 -> !inverse ? MarkerSign.ATTACK2 : MarkerSign.BIND2;
                                case 3 -> !inverse ? MarkerSign.ATTACK3 : MarkerSign.BIND3;
                                case 4 -> !inverse ? MarkerSign.ATTACK4 : MarkerSign.SQUARE;
                                default -> MarkerSign.IGNORE_NEXT; //Uh oh stinky
                            }));
                            s.accept(new SpecificAutoMarkRequest(supPlayer, switch (i) {
                                case 1 -> inverse ? MarkerSign.ATTACK1 : MarkerSign.BIND1;
                                case 2 -> inverse ? MarkerSign.ATTACK2 : MarkerSign.BIND2;
                                case 3 -> inverse ? MarkerSign.ATTACK3 : MarkerSign.BIND3;
                                case 4 -> inverse ? MarkerSign.ATTACK4 : MarkerSign.SQUARE;
                                default -> MarkerSign.IGNORE_NEXT; //Uh oh stinky
                            }));
                        }
                    }
                }
            });

    public BooleanSetting getUseAutoMarks() {
        return useAutoMarks;
    }

    public BooleanSetting getUseLimitlessDesolation() {
        return useLimitlessDesolation;
    }

    public BooleanSetting getUseHC1() {
        return useHC1;
    }

    public BooleanSetting getUseHC2() {
        return useHC2;
    }

    public BooleanSetting getUseDominion() {
        return useLD;
    }

    public BooleanSetting getLDSupportAttack() {
        return LDSupportAttack;
    }
}
