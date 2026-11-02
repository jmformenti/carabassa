package org.atypical.carabassa.cli.command;

import org.atypical.carabassa.cli.exception.ApiException;
import org.atypical.carabassa.cli.service.DatasetApiService;
import org.atypical.carabassa.restapi.representation.model.ItemRepresentation;
import org.atypical.carabassa.restapi.representation.model.TagEntityRepresentation;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TagCommandsTest {

    private final DatasetApiService api = mock(DatasetApiService.class);

    private int run(Object command, String... args) {
        ReflectionTestUtils.setField(command, "datasetApiService", api);
        return new CommandLine(command).execute(args);
    }

    @Test
    void noSelectionIsRejected() throws Exception {
        when(api.findByName("ds")).thenReturn(1L);
        assertEquals(ExitCode.USAGE, run(new DeleteTagCommand(), "-d", "ds", "-n", "t", "-f"));
        assertEquals(ExitCode.USAGE, run(new AddTagCommand(), "-d", "ds", "-n", "t", "-v", "x", "-f"));
        assertEquals(ExitCode.USAGE, run(new ListTagsCommand(), "-d", "ds"));
        verify(api, never()).findItems(anyLong(), any(), anyBoolean());
        verify(api, never()).deleteItemTag(anyLong(), anyLong(), anyLong());
        verify(api, never()).addItemTag(anyLong(), anyLong(), anyString(), any(), any());
    }

    @Test
    void deleteTagFiltersByValue() throws Exception {
        when(api.findByName("ds")).thenReturn(1L);
        ItemRepresentation item = mock(ItemRepresentation.class);
        when(item.getId()).thenReturn(5L);
        TagEntityRepresentation a = tag(10L, "t", "a");
        TagEntityRepresentation b = tag(11L, "t", "b");
        SortedSet<TagEntityRepresentation> both = tags(a, b);
        when(item.getTags()).thenReturn(both);
        when(api.findItem(1L, 5L)).thenReturn(item);

        assertEquals(ExitCode.OK, run(new DeleteTagCommand(), "-d", "ds", "-i", "5", "-n", "t", "-v", "a", "-f"));
        verify(api).deleteItemTag(1L, 5L, 10L);
        verify(api, never()).deleteItemTag(1L, 5L, 11L);
    }

    @Test
    void deleteTagApiErrorGivesSoftwareExitCode() throws Exception {
        when(api.findByName("ds")).thenReturn(1L);
        ItemRepresentation item = mock(ItemRepresentation.class);
        when(item.getId()).thenReturn(5L);
        SortedSet<TagEntityRepresentation> one = tags(tag(10L, "t", "a"));
        when(item.getTags()).thenReturn(one);
        when(api.findItem(1L, 5L)).thenReturn(item);
        org.mockito.Mockito.doThrow(new ApiException("boom")).when(api).deleteItemTag(1L, 5L, 10L);

        assertEquals(ExitCode.SOFTWARE, run(new DeleteTagCommand(), "-d", "ds", "-i", "5", "-n", "t", "-f"));
    }

    private static SortedSet<TagEntityRepresentation> tags(TagEntityRepresentation... tags) {
        SortedSet<TagEntityRepresentation> set = new TreeSet<>(Comparator.comparing(TagEntityRepresentation::getId));
        set.addAll(List.of(tags));
        return set;
    }

    private static TagEntityRepresentation tag(Long id, String name, Object value) {
        TagEntityRepresentation t = mock(TagEntityRepresentation.class);
        when(t.getId()).thenReturn(id);
        when(t.getName()).thenReturn(name);
        when(t.getValue()).thenReturn(value);
        return t;
    }
}
